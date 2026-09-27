package com.maggotkingtriptracker.view;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.model.AccountHistory;
import com.maggotkingtriptracker.model.EggPop;
import com.maggotkingtriptracker.model.CorpseChoice;
import com.maggotkingtriptracker.model.ItemEntry;
import com.maggotkingtriptracker.model.KillGoal;
import com.maggotkingtriptracker.model.Kill;
import com.maggotkingtriptracker.model.Trip;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.pricing.PriceService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public GoalView goal(AccountHistory history, boolean running, long now)
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
		return new GoalView(goal.getTarget(), done, goal.getActiveMs(), now, running);
	}

	public TripView trip(Trip trip)
	{
		List<ItemEntry> loot = new ArrayList<>();
		boolean pet = false;
		for (Kill kill : trip.getKills())
		{
			loot.addAll(kill.getLoot());
			pet |= kill.isPet();
		}

		return TripView.builder()
			.id(trip.getId())
			.startedAt(trip.getStartedAt())
			.endedAt(trip.getEndedAt())
			.endReason(trip.getEndReason())
			.activeMs(trip.getActiveMs())
			.segmentStartedAt(trip.getSegmentStartedAt())
			.kills(trip.getKills().size())
			.stomachKills(TripMath.countChoice(trip, CorpseChoice.STOMACH))
			.eggKills(TripMath.countChoice(trip, CorpseChoice.EGGS))
			.deaths(trip.getDeaths().size())
			.pet(pet)
			.lootValue(TripMath.lootValue(trip))
			.supplyCost(TripMath.supplyCost(trip))
			.droppedCost(TripMath.droppedCost(trip))
			.deathCost(TripMath.deathCost(trip))
			.netProfit(TripMath.netProfit(trip))
			.averageKillMs(TripMath.averageKillMs(Collections.singletonList(trip)))
			.loot(items(loot))
			.supplies(items(trip.getSupplies()))
			.dropped(items(trip.getDropped()))
			.supplyCategories(supplyCategories(trip.getSupplies()))
			.build();
	}

	/**
	 * @param now current time, for the running segment of an open trip
	 */
	public LifetimeView lifetime(AccountHistory history, boolean includeTodayValue, long now)
	{
		List<Trip> trips = history.getTrips();
		int kills = 0;
		int stomach = 0;
		int eggs = 0;
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
			stomach += TripMath.countChoice(trip, CorpseChoice.STOMACH);
			eggs += TripMath.countChoice(trip, CorpseChoice.EGGS);
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

		return LifetimeView.builder()
			.trips(trips.size())
			.kills(kills)
			.stomachKills(stomach)
			.eggKills(eggs)
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
			.dryness(dryness(history))
			.polish(polish(history))
			.build();
	}

	private DrynessView dryness(AccountHistory history)
	{
		int stomachKills = 0;
		int sinceUnique = 0;
		int petsFromKills = 0;
		Map<Integer, List<Integer>> received = new LinkedHashMap<>();
		for (int uniqueId : MaggotKingRates.UNIQUES.keySet())
		{
			received.put(uniqueId, new ArrayList<>());
		}

		for (Trip trip : history.getTrips())
		{
			for (Kill kill : trip.getKills())
			{
				if (kill.isPet())
				{
					petsFromKills++;
				}
				if (kill.getChoice() != CorpseChoice.STOMACH)
				{
					continue;
				}
				stomachKills++;
				sinceUnique++;
				for (ItemEntry entry : kill.getLoot())
				{
					List<Integer> kcs = received.get(entry.getItemId());
					if (kcs != null)
					{
						for (long i = 0; i < entry.getQuantity(); i++)
						{
							kcs.add(kill.getKillCount());
						}
						sinceUnique = 0;
					}
				}
			}
		}

		List<DrynessView.Unique> uniques = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> e : received.entrySet())
		{
			uniques.add(new DrynessView.Unique(e.getKey(), prices.name(e.getKey()),
				stomachKills * MaggotKingRates.UNIQUES.get(e.getKey()), e.getValue()));
		}

		Map<Integer, int[]> eggCounts = new LinkedHashMap<>();
		for (int eggId : MaggotKingRates.EGG_PET.keySet())
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
		int petsFromEggs = 0;
		for (Map.Entry<Integer, int[]> e : eggCounts.entrySet())
		{
			double rate = MaggotKingRates.EGG_PET.get(e.getKey());
			tiers.add(new DrynessView.EggTier(e.getKey(), prices.name(e.getKey()), e.getValue()[0], e.getValue()[1], rate));
			noPetFromEggs *= Math.pow(1 - rate, e.getValue()[0]);
			petsFromEggs += e.getValue()[1];
		}

		return new DrynessView(stomachKills, sinceUnique, Math.pow(1 - MaggotKingRates.ANY_UNIQUE, sinceUnique),
			uniques, stomachKills * MaggotKingRates.PET_PER_STOMACH, petsFromKills, tiers, 1 - noPetFromEggs, petsFromEggs);
	}

	private List<PolishView> polish(AccountHistory history)
	{
		List<PolishView> views = new ArrayList<>();
		for (int tarnishedId : MaggotKingIds.TARNISHED_ITEMS)
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

	private List<ItemView> items(Collection<ItemEntry> entries)
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

		List<ItemView> views = new ArrayList<>();
		for (Map.Entry<String, ItemEntry> e : firsts.entrySet())
		{
			ItemEntry first = e.getValue();
			long[] sum = totals.get(e.getKey());
			int itemId = first.getItemId();
			views.add(new ItemView(itemId, prices.name(itemId), sum[0], sum[1], first.isPerDose(),
				MaggotKingIds.UNIQUES.contains(itemId), first.isPending(),
				first.isCharges() ? prices.name(first.getChargeItemId()) : null, first.getChargesPerItem(),
				first.getPolishedFrom() > 0 ? prices.name(first.getPolishedFrom()) : null));
		}
		views.sort(BY_VALUE);
		return views;
	}
}
