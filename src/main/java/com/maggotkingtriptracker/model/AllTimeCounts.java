package com.maggotkingtriptracker.model;

import java.util.Map;
import lombok.Value;

/**
 * A boss's all-time records from RuneLite's core plugins: the Loot Tracker's kills and drops, and the kill count
 * saved by Chat Commands.
 */
@Value
public class AllTimeCounts
{
	/**
	 * Kills recorded by the Loot Tracker (loot-dropping kills; for the Maggot King, Open-stomach).
	 */
	int lootKills;
	/**
	 * The game's kill count, if Chat Commands has seen it; null otherwise.
	 */
	Integer killCount;
	long firstRecordedAt;
	Map<Integer, Integer> drops;

	public int dropped(int itemId)
	{
		return drops.getOrDefault(itemId, 0);
	}
}
