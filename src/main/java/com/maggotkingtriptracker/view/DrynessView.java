package com.maggotkingtriptracker.view;

import java.util.List;
import lombok.Value;

@Value
public class DrynessView
{
	@Value
	public static class Unique
	{
		int itemId;
		String name;
		double expected;
		/**
		 * Kill counts it was received at, oldest first; null entries mean the kill count wasn't seen.
		 */
		List<Integer> killCounts;
	}

	@Value
	public static class EggTier
	{
		int itemId;
		String name;
		int popped;
		int pets;
		double petRate;
	}

	int stomachKills;
	int stomachKillsSinceUnique;
	/**
	 * Chance of going this many Open-stomach kills without a unique.
	 */
	double chanceThisDry;
	List<Unique> uniques;
	double expectedPetsFromKills;
	int petsFromKills;
	List<EggTier> eggTiers;
	/**
	 * Chance of at least one pet from all the eggs popped so far.
	 */
	double eggPetChance;
	int petsFromEggs;
	/**
	 * Highest kill count seen in tracked kills; null if none reported one.
	 */
	Integer currentKc;
	/**
	 * Kill count of the most recent unique; null if none was tracked.
	 */
	Integer lastUniqueKc;
	/**
	 * Kill count of the first tracked kill, the start of the plugin's records.
	 */
	Integer firstTrackedKc;
	int uniquesReceived;
	double expectedUniques;
}
