package com.maggotkingtriptracker.view;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.model.CorpseChoice;
import com.maggotkingtriptracker.model.ItemEntry;
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

	public ViewBuilder(PriceService prices)
	{
		this.prices = prices;
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
			.build();
	}

	/**
	 * @param trips every stored trip, including one in progress
	 * @param now current time, for the running segment of an open trip
	 */
	public LifetimeView lifetime(List<Trip> trips, boolean includeTodayValue, long now)
	{
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
			.build();
	}

	private List<ItemView> items(Collection<ItemEntry> entries)
	{
		// Combine lines for the same item; pending tarnished drops stay separate
		Map<String, long[]> totals = new LinkedHashMap<>();
		Map<String, ItemEntry> firsts = new LinkedHashMap<>();
		for (ItemEntry entry : entries)
		{
			String key = entry.getItemId() + (entry.isPerDose() ? "d" : "") + (entry.isPending() ? "p" : "")
				+ (entry.isCharges() ? "c" + entry.getChargeItemId() : "");
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
				first.isCharges() ? prices.name(first.getChargeItemId()) : null, first.getChargesPerItem()));
		}
		views.sort(BY_VALUE);
		return views;
	}
}
