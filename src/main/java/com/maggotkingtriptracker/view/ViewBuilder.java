package com.maggotkingtriptracker.view;

import com.maggotkingtriptracker.boss.BossDefinition;
import com.maggotkingtriptracker.boss.DropKind;
import com.maggotkingtriptracker.boss.ExpectedDrop;
import com.maggotkingtriptracker.boss.KillContext;
import com.maggotkingtriptracker.boss.LootChoice;
import com.maggotkingtriptracker.boss.TripStat;
import com.maggotkingtriptracker.model.AllTimeCounts;
import com.maggotkingtriptracker.model.ChargeType;
import com.maggotkingtriptracker.model.BossHistory;
import com.maggotkingtriptracker.model.DryStreak;
import com.maggotkingtriptracker.model.EggPop;
import com.maggotkingtriptracker.model.ItemEntry;
import com.maggotkingtriptracker.model.Kill;
import com.maggotkingtriptracker.model.KillGoal;
import com.maggotkingtriptracker.model.Trip;
import com.maggotkingtriptracker.model.TripClock;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.model.VariantFilter;
import com.maggotkingtriptracker.pricing.PriceService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.util.QuantityFormatter;

/**
 * Turns stored trips into immutable view objects for the panel. Runs on the client thread,
 * because item names come from ItemManager.
 */
public class ViewBuilder
{
	private static final Comparator<ItemView> BY_VALUE = Comparator
		.comparing(ItemView::isUnique).reversed()
		.thenComparing(Comparator.comparingLong(ItemView::getTotalValue).reversed())
		.thenComparing(ItemView::getName);

	private final PriceService prices;
	private Set<Integer> runeIds = Collections.emptySet();

	public ViewBuilder(PriceService prices)
	{
		this.prices = prices;
	}

	/**
	 * Item ids that count as runes in the supply breakdown (every rune the rune pouch can hold).
	 */
	public void setRuneIds(Set<Integer> runeIds)
	{
		this.runeIds = runeIds;
	}

	/**
	 * @param currentTrip this boss's open trip, whose running segment keeps the goal clock going; null if none
	 */
	public GoalView goal(BossHistory history, Trip currentTrip, long now)
	{
		KillGoal goal = history.getGoal();
		if (goal == null)
		{
			return null;
		}

		int done = 0;
		for (Trip trip : history.getTrips())
		{
			for (Kill kill : trip.getKills())
			{
				if (kill.getEndedAt() >= goal.getStartedAt())
				{
					done++;
				}
			}
		}
		Long segmentStart = TripClock.goalSegmentStart(goal, currentTrip);
		return new GoalView(goal.getTarget(), done, goal.getActiveMs(), segmentStart != null ? segmentStart : now,
			segmentStart != null);
	}

	public TripView trip(BossDefinition boss, Trip trip)
	{
		List<ItemEntry> loot = new ArrayList<>();
		boolean pet = false;
		for (Kill kill : trip.getKills())
		{
			loot.addAll(kill.getLoot());
			pet |= kill.isPet();
		}

		TripStat stat = boss.getProfitCell();
		return TripView.builder()
			.id(trip.getId())
			.startedAt(trip.getStartedAt())
			.endedAt(trip.getEndedAt())
			.endReason(trip.getEndReason())
			.activeMs(trip.getActiveMs())
			.segmentStartedAt(trip.getSegmentStartedAt())
			.kills(trip.getKills().size())
			.bossStat(new StatView(stat.getLabel(), stat.valueOf(trip), stat.getHelp()))
			.deaths(trip.getDeaths().size())
			.pet(pet)
			.lootValue(TripMath.lootValue(trip))
			.supplyCost(TripMath.supplyCost(trip))
			.droppedCost(TripMath.droppedCost(trip))
			.deathCost(TripMath.deathCost(trip))
			.netProfit(TripMath.netProfit(trip))
			.averageKillMs(TripMath.averageKillMs(Collections.singletonList(trip)))
			.fastestKillMs(TripMath.fastestKillMs(Collections.singletonList(trip)))
			.lastKillMs(lastKillMs(trip))
			.loot(items(boss, loot))
			.supplies(items(boss, trip.getSupplies()))
			.dropped(items(boss, trip.getDropped()))
			.supplyCategories(supplyCategories(trip.getSupplies()))
			.build();
	}

	private static Long lastKillMs(Trip trip)
	{
		for (int i = trip.getKills().size() - 1; i >= 0; i--)
		{
			Long duration = trip.getKills().get(i).getDurationMs();
			if (duration != null)
			{
				return duration;
			}
		}
		return null;
	}

	/**
	 * @param variant variant chip selected, or null for All
	 * @param allTime records from RuneLite's core plugins; null if there are none
	 * @param now current time, for the running segment of an open trip
	 */
	public LifetimeView lifetime(BossDefinition boss, BossHistory history, String variant, AllTimeCounts allTime,
		boolean includeTodayValue, long now)
	{
		List<Trip> trips = new ArrayList<>();
		for (Trip trip : history.getTrips())
		{
			if (VariantFilter.matches(trip, variant))
			{
				trips.add(trip);
			}
		}

		int kills = 0;
		int deaths = 0;
		int pets = 0;
		long activeMs = 0;
		long loot = 0;
		long supplies = 0;
		long dropped = 0;
		long deathCost = 0;
		long today = 0;
		List<Long> netPerTrip = new ArrayList<>();

		for (Trip trip : trips)
		{
			kills += trip.getKills().size();
			deaths += trip.getDeaths().size();
			activeMs += trip.activeMsAt(now);
			loot += TripMath.lootValue(trip);
			supplies += TripMath.supplyCost(trip);
			dropped += TripMath.droppedCost(trip);
			deathCost += TripMath.deathCost(trip);
			for (Kill kill : trip.getKills())
			{
				if (kill.isPet())
				{
					pets++;
				}
				if (includeTodayValue)
				{
					for (ItemEntry entry : kill.getLoot())
					{
						if (!entry.isPending())
						{
							today += entry.getQuantity() * prices.price(entry.getItemId());
						}
					}
				}
			}
			if (!trip.isOpen())
			{
				netPerTrip.add(TripMath.netProfit(trip));
			}
		}

		List<String> choices = new ArrayList<>();
		for (LootChoice choice : boss.getLootChoices())
		{
			int count = 0;
			for (Trip trip : trips)
			{
				count += TripMath.countChoice(trip, choice.getKey());
			}
			choices.add(choice.getLabel() + " " + count);
		}

		return LifetimeView.builder()
			.trips(trips.size())
			.kills(kills)
			.choiceSummary(String.join(" · ", choices))
			.deaths(deaths)
			.pets(pets)
			.activeMs(activeMs)
			.lootValue(loot)
			.supplyCost(supplies)
			.droppedCost(dropped)
			.deathCost(deathCost)
			.netProfit(loot - supplies - dropped - deathCost)
			.averageKillMs(TripMath.averageKillMs(trips))
			.lootValueToday(includeTodayValue ? today : null)
			.netPerTrip(netPerTrip)
			.dryness(dryness(boss, history, trips, variant, allTime))
			.polish(polish(boss, history))
			.build();
	}

	DrynessView dryness(BossDefinition boss, BossHistory history, List<Trip> trips, String variant,
		AllTimeCounts allTime)
	{
		List<ExpectedDrop> uniqueDrops = new ArrayList<>();
		for (ExpectedDrop drop : boss.getDrops())
		{
			if (drop.getKind() == DropKind.UNIQUE)
			{
				uniqueDrops.add(drop);
			}
		}
		ExpectedDrop petDrop = boss.getPet();

		List<Kill> kills = new ArrayList<>();
		int luckKills = 0;
		int petsFromKills = 0;
		Integer currentKc = null;
		Integer firstTrackedKc = null;
		double expectedAny = 0;
		double expectedPet = 0;
		double[] expected = new double[uniqueDrops.size()];
		Map<Integer, List<Integer>> received = new LinkedHashMap<>();
		for (ExpectedDrop drop : uniqueDrops)
		{
			received.put(drop.getItemId(), new ArrayList<>());
		}

		for (Trip trip : trips)
		{
			for (Kill kill : trip.getKills())
			{
				if (!VariantFilter.matches(kill, variant))
				{
					continue;
				}
				kills.add(kill);
				if (kill.isPet())
				{
					petsFromKills++;
				}
				if (kill.getKillCount() != null)
				{
					currentKc = currentKc == null ? kill.getKillCount() : Math.max(currentKc, kill.getKillCount());
					if (firstTrackedKc == null)
					{
						firstTrackedKc = kill.getKillCount();
					}
				}
				if (!boss.countsForLuck(kill))
				{
					continue;
				}

				// Each kill at its own rate, so "All" mixes variants correctly
				KillContext context = KillContext.of(kill);
				luckKills++;
				expectedAny += boss.anyUniqueChance(context);
				expectedPet += petDrop == null ? 0 : petDrop.chance(context);
				for (int i = 0; i < uniqueDrops.size(); i++)
				{
					expected[i] += uniqueDrops.get(i).chance(context);
				}
				for (ItemEntry entry : kill.getLoot())
				{
					List<Integer> kcs = received.get(entry.getItemId());
					if (kcs != null)
					{
						for (long i = 0; i < entry.getQuantity(); i++)
						{
							kcs.add(kill.getKillCount());
						}
					}
				}
			}
		}

		int uniquesReceived = 0;
		List<DrynessView.Drop> uniques = new ArrayList<>();
		for (int i = 0; i < uniqueDrops.size(); i++)
		{
			ExpectedDrop drop = uniqueDrops.get(i);
			List<Integer> kcs = received.get(drop.getItemId());
			uniquesReceived += kcs.size();
			uniques.add(new DrynessView.Drop(drop.getItemId(), prices.name(drop.getItemId()),
				averageRate(expected[i], luckKills, drop.chance(KillContext.DEFAULT)), expected[i], kcs.size(), kcs));
		}
		DrynessView.Drop pet = petDrop == null ? null : new DrynessView.Drop(petDrop.getItemId(),
			prices.name(petDrop.getItemId()), averageRate(expectedPet, luckKills, petDrop.chance(KillContext.DEFAULT)),
			expectedPet, petsFromKills, Collections.emptyList());

		Map<Integer, int[]> eggCounts = new LinkedHashMap<>();
		for (int eggId : boss.getEggPetRates().keySet())
		{
			eggCounts.put(eggId, new int[2]);
		}
		for (EggPop pop : history.getEggPops())
		{
			int[] counts = eggCounts.get(pop.getEggItemId());
			if (counts != null)
			{
				counts[0]++;
				counts[1] += pop.isPet() ? 1 : 0;
			}
		}

		List<DrynessView.EggTier> tiers = new ArrayList<>();
		double noPetFromEggs = 1;
		double eggPetExpected = 0;
		int petsFromEggs = 0;
		for (Map.Entry<Integer, int[]> e : eggCounts.entrySet())
		{
			double rate = boss.getEggPetRates().get(e.getKey());
			tiers.add(new DrynessView.EggTier(e.getKey(), prices.name(e.getKey()), e.getValue()[0], e.getValue()[1], rate));
			noPetFromEggs *= Math.pow(1 - rate, e.getValue()[0]);
			eggPetExpected += e.getValue()[0] * rate;
			petsFromEggs += e.getValue()[1];
		}

		List<Integer> uniqueKcs = new ArrayList<>();
		for (List<Integer> kcs : received.values())
		{
			for (Integer kc : kcs)
			{
				if (kc != null)
				{
					uniqueKcs.add(kc);
				}
			}
		}

		double anyRate = averageRate(expectedAny, luckKills, boss.anyUniqueChance(KillContext.DEFAULT));
		DryStreak.Result streak = DryStreak.compute(kills, boss::countsForLuck,
			kill -> kill.getLoot().stream().anyMatch(e -> boss.isUnique(e.getItemId())),
			history.getLastUniqueKc(), allTime == null ? null : allTime.getKillCount());

		return DrynessView.builder()
			.luckKills(luckKills)
			.killsSinceUnique(streak.getSince())
			.sinceFromEnteredKc(streak.isFromEnteredKc())
			.longestDryStreak(DryStreak.longest(uniqueKcs, history.getLastUniqueKc(), streak.getSince()))
			.chanceThisDry(Math.pow(1 - anyRate, streak.getSince()))
			.anyUniqueRate(anyRate)
			.uniquesReceived(uniquesReceived)
			.expectedUniques(expectedAny)
			.uniques(uniques)
			.pet(pet)
			.eggTiers(tiers)
			.eggPetChance(1 - noPetFromEggs)
			.eggPetExpected(eggPetExpected)
			.petsFromEggs(petsFromEggs)
			.currentKc(currentKc)
			.lastUniqueKc(streak.getLastUniqueKc())
			.firstTrackedKc(firstTrackedKc)
			.enteredLastUniqueKc(history.getLastUniqueKc())
			.allTime(allTime(boss, uniqueDrops, allTime, kills, currentKc, petsFromKills + petsFromEggs, eggPetExpected))
			.build();
	}

	/**
	 * All-time figures at the default rates: the Loot Tracker doesn't record the variant or party size of a kill.
	 */
	private DrynessView.AllTime allTime(BossDefinition boss, List<ExpectedDrop> uniqueDrops, AllTimeCounts counts,
		List<Kill> trackedKills, Integer trackedKc, int trackedPets, double eggPetExpected)
	{
		if (counts == null)
		{
			return null;
		}

		// The Loot Tracker saves its record some seconds after a drop. Tracked loot kills since its last save aren't
		// in it yet, so add them, or a unique would only show up after the save
		int kills = counts.getLootKills();
		Map<Integer, Integer> unsaved = new LinkedHashMap<>();
		if (counts.getLastRecordedAt() > 0)
		{
			for (Kill kill : trackedKills)
			{
				if (kill.getEndedAt() > counts.getLastRecordedAt() && boss.countsForLuck(kill))
				{
					kills++;
					for (ItemEntry entry : kill.getLoot())
					{
						unsaved.merge(entry.getItemId(), (int) entry.getQuantity(), Integer::sum);
					}
				}
			}
		}

		int received = 0;
		List<DrynessView.Drop> uniques = new ArrayList<>();
		for (ExpectedDrop drop : uniqueDrops)
		{
			double rate = drop.chance(KillContext.DEFAULT);
			int got = counts.dropped(drop.getItemId()) + unsaved.getOrDefault(drop.getItemId(), 0);
			received += got;
			uniques.add(new DrynessView.Drop(drop.getItemId(), prices.name(drop.getItemId()), rate, kills * rate, got,
				Collections.emptyList()));
		}

		ExpectedDrop petDrop = boss.getPet();
		DrynessView.Drop pet = null;
		if (petDrop != null)
		{
			double rate = petDrop.chance(KillContext.DEFAULT);
			// The Loot Tracker doesn't record every pet, so take the larger count
			pet = new DrynessView.Drop(petDrop.getItemId(), prices.name(petDrop.getItemId()), rate,
				kills * rate + eggPetExpected, Math.max(counts.dropped(petDrop.getItemId()), trackedPets),
				Collections.emptyList());
		}

		return DrynessView.AllTime.builder()
			.lootKills(kills)
			// Chat Commands may also be a kill behind
			.killCount(counts.getKillCount() == null ? trackedKc
				: trackedKc == null ? counts.getKillCount() : Integer.valueOf(Math.max(counts.getKillCount(), trackedKc)))
			.firstRecordedAt(counts.getFirstRecordedAt())
			.uniquesReceived(received)
			.expectedUniques(kills * boss.anyUniqueChance(KillContext.DEFAULT))
			.uniques(uniques)
			.pet(pet)
			.build();
	}

	/**
	 * The average chance per kill, or the default rate when there are no kills or every kill had it.
	 */
	static double averageRate(double expected, int kills, double defaultRate)
	{
		if (kills == 0)
		{
			return defaultRate;
		}
		double average = expected / kills;
		return Math.abs(average - defaultRate) < 1e-12 ? defaultRate : average;
	}

	private List<PolishView> polish(BossDefinition boss, BossHistory history)
	{
		List<PolishView> views = new ArrayList<>();
		for (int tarnishedId : boss.getTarnishedItems())
		{
			Map<Integer, Integer> outcomes = history.getPolishOutcomes().get(tarnishedId);
			if (outcomes == null || outcomes.isEmpty())
			{
				continue;
			}

			int total = 0;
			List<ItemView> items = new ArrayList<>();
			for (Map.Entry<Integer, Integer> e : outcomes.entrySet())
			{
				int count = e.getValue();
				total += count;
				items.add(new ItemView(e.getKey(), prices.name(e.getKey()), count, count * prices.price(e.getKey()),
					false, false, false, null, 0, null));
			}
			items.sort(Comparator.comparingLong(ItemView::getQuantity).reversed());
			views.add(new PolishView(tarnishedId, prices.name(tarnishedId), total, items));
		}
		return views;
	}

	private List<SupplyCategory> supplyCategories(List<ItemEntry> supplies)
	{
		long charges = 0;
		long runes = 0;
		long potions = 0;
		long food = 0;
		long other = 0;
		for (ItemEntry entry : supplies)
		{
			long value = entry.totalValue();
			if (entry.isCharges())
			{
				charges += value;
			}
			else if (runeIds.contains(entry.getItemId()))
			{
				runes += value;
			}
			else if (entry.isPerDose())
			{
				potions += value;
			}
			else if (prices.isFood(entry.getItemId()))
			{
				food += value;
			}
			else
			{
				other += value;
			}
		}

		List<SupplyCategory> categories = new ArrayList<>();
		addCategory(categories, "Charges", charges);
		addCategory(categories, "Runes", runes);
		addCategory(categories, "Potions", potions);
		addCategory(categories, "Food", food);
		addCategory(categories, "Other", other);
		return categories;
	}

	private static void addCategory(List<SupplyCategory> categories, String name, long value)
	{
		if (value > 0)
		{
			categories.add(new SupplyCategory(name, value));
		}
	}

	/**
	 * What recharges a charge line, e.g. "Blood shard" or "Vial of blood + 300 Blood rune".
	 */
	private String rechargeName(ItemEntry entry)
	{
		ChargeType type = ChargeType.forSourceItem(entry.getItemId());
		if (type == null || type.getComponents().size() == 1)
		{
			return prices.name(entry.getChargeItemId());
		}
		List<String> parts = new ArrayList<>();
		for (ChargeType.Component component : type.getComponents())
		{
			parts.add((component.getQuantity() > 1 ? QuantityFormatter.formatNumber(component.getQuantity()) + " " : "")
				+ prices.name(component.getItemId()));
		}
		return String.join(" + ", parts);
	}

	private List<ItemView> items(BossDefinition boss, Collection<ItemEntry> entries)
	{
		// Combine lines for the same item; pending tarnished drops stay separate
		Map<String, long[]> totals = new LinkedHashMap<>();
		Map<String, ItemEntry> firsts = new LinkedHashMap<>();
		for (ItemEntry entry : entries)
		{
			String key = entry.getItemId() + (entry.isPerDose() ? "d" : "") + (entry.isPending() ? "p" : "")
				+ (entry.isCharges() ? "c" + entry.getChargeItemId() : "")
				+ (entry.getPolishedFrom() > 0 ? "f" + entry.getPolishedFrom() : "");
			long[] sum = totals.computeIfAbsent(key, k -> new long[2]);
			sum[0] += entry.getQuantity();
			sum[1] += entry.totalValue();
			firsts.putIfAbsent(key, entry);
		}

		Set<Integer> highlighted = boss.getHighlightedItems();
		List<ItemView> views = new ArrayList<>();
		for (Map.Entry<String, ItemEntry> e : firsts.entrySet())
		{
			ItemEntry first = e.getValue();
			long[] sum = totals.get(e.getKey());
			int itemId = first.getItemId();
			views.add(new ItemView(itemId, prices.name(itemId), sum[0], sum[1], first.isPerDose(),
				highlighted.contains(itemId), first.isPending(),
				first.isCharges() ? rechargeName(first) : null, first.getChargesPerItem(),
				first.getPolishedFrom() > 0 ? prices.name(first.getPolishedFrom()) : null));
		}
		views.sort(BY_VALUE);
		return views;
	}
}
