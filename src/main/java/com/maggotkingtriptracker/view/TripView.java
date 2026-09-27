package com.maggotkingtriptracker.view;

import com.maggotkingtriptracker.model.TripEndReason;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TripView
{
	String id;
	long startedAt;
	Long endedAt;
	TripEndReason endReason;
	long activeMs;
	/**
	 * Start of the running in-lair segment, for the live timer; null when not in the lair.
	 */
	Long segmentStartedAt;
	int kills;
	int stomachKills;
	int eggKills;
	int deaths;
	boolean pet;
	long lootValue;
	long supplyCost;
	long droppedCost;
	long deathCost;
	long netProfit;
	Long averageKillMs;
	Long fastestKillMs;
	List<ItemView> loot;
	List<ItemView> supplies;
	List<ItemView> dropped;
	/**
	 * Supply cost split into charges, runes, potions, food and other; only non-zero categories.
	 */
	List<SupplyCategory> supplyCategories;

	public long activeMsAt(long now)
	{
		return activeMs + (segmentStartedAt != null ? Math.max(0, now - segmentStartedAt) : 0);
	}
}
