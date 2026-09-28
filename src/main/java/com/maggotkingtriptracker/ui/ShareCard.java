package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.boss.BossDefinition;
import com.maggotkingtriptracker.boss.BossVariant;
import com.maggotkingtriptracker.model.LuckTier;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.DrynessView;
import com.maggotkingtriptracker.view.LifetimeView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * What goes on a share card, taken from the panel's state for the shown boss and variant.
 */
@Value
@Builder
class ShareCard
{
	static final int RECENT_TRIPS = 5;

	@Value
	static class Drop
	{
		int itemId;
		String name;
		int count;
	}

	@Value
	static class TripLine
	{
		long startedAt;
		int kills;
		long net;
	}

	String bossName;
	/**
	 * Selected variant, e.g. "Hard"; null for All or bosses without variants.
	 */
	String variant;
	int iconItemId;
	/**
	 * Null when names are turned off or unknown.
	 */
	String playerName;
	/**
	 * The game's kill count; null if unknown.
	 */
	Integer killCount;
	int uniquesReceived;
	double uniquesExpected;
	/**
	 * Null without kills.
	 */
	LuckTier tier;
	/**
	 * The luck numbers come from RuneLite's all-time records rather than tracked kills.
	 */
	boolean allTime;
	int dryKills;
	/**
	 * Each unique, then the pet.
	 */
	List<Drop> drops;
	long loot;
	long costs;
	long net;
	long gpPerHour;
	int trips;
	List<TripLine> recentTrips;
	long createdAt;

	/**
	 * @return the card, or null if there is nothing to show (not logged in)
	 */
	static ShareCard from(PanelState state, boolean showName, long now)
	{
		LifetimeView lifetime = state.getLifetime();
		if (lifetime == null)
		{
			return null;
		}
		BossDefinition boss = state.getBoss();
		DrynessView dryness = lifetime.getDryness();
		LuckSummary luck = LuckSummary.of(dryness);

		List<Drop> drops = new ArrayList<>();
		for (DrynessView.Drop unique : luck.getUniques())
		{
			drops.add(new Drop(unique.getItemId(), unique.getName(), unique.getReceived()));
		}
		DrynessView.Drop pet = LuckSummary.pet(dryness);
		if (pet != null)
		{
			drops.add(new Drop(pet.getItemId(), pet.getName(), pet.getReceived()));
		}

		List<TripLine> recent = new ArrayList<>();
		for (TripView trip : state.getHistory())
		{
			if (recent.size() == RECENT_TRIPS)
			{
				break;
			}
			recent.add(new TripLine(trip.getStartedAt(), trip.getKills(), trip.getNetProfit()));
		}

		String variant = null;
		for (BossVariant v : boss.getVariants())
		{
			if (v.getId().equals(state.getVariant()))
			{
				variant = v.getLabel();
			}
		}

		Integer killCount = dryness.getAllTime() != null && dryness.getAllTime().getKillCount() != null
			? dryness.getAllTime().getKillCount() : dryness.getCurrentKc();
		long costs = lifetime.getSupplyCost() + lifetime.getDroppedCost() + lifetime.getDeathCost();
		return ShareCard.builder()
			.bossName(boss.getDisplayName())
			.variant(variant)
			.iconItemId(boss.getIconItemId())
			.playerName(showName ? state.getPlayerName() : null)
			.killCount(killCount)
			.uniquesReceived(luck.getReceived())
			.uniquesExpected(luck.getExpected())
			.tier(luck.getTier())
			.allTime(luck.isAllTime())
			.dryKills(dryness.getKillsSinceUnique())
			.drops(drops)
			.loot(lifetime.getLootValue())
			.costs(costs)
			.net(lifetime.getNetProfit())
			.gpPerHour(TripMath.gpPerHour(lifetime.getNetProfit(), lifetime.getActiveMs()))
			.trips(lifetime.getTrips())
			.recentTrips(recent)
			.createdAt(now)
			.build();
	}
}
