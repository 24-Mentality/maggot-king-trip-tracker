package com.maggotkingtriptracker.view;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LifetimeView
{
	int trips;
	int kills;
	int stomachKills;
	int eggKills;
	int deaths;
	int pets;
	long activeMs;
	long lootValue;
	long supplyCost;
	long droppedCost;
	long deathCost;
	long netProfit;
	Long averageKillMs;
	/**
	 * All loot valued at today's GE prices; null unless enabled in config.
	 */
	Long lootValueToday;
	/**
	 * Net profit of completed trips, oldest first.
	 */
	List<Long> netPerTrip;
}
