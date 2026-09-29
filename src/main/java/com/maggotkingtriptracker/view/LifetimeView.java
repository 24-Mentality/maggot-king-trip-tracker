package com.maggotkingtriptracker.view;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class LifetimeView
{
	int trips;
	int kills;
	/**
	 * Kills per loot choice, e.g. "Stomach 10 · Eggs 2"; empty for bosses without choices.
	 */
	String choiceSummary;
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
	DrynessView dryness;
	/**
	 * Tarnished items polished; empty for bosses without them.
	 */
	List<PolishView> polish;
}
