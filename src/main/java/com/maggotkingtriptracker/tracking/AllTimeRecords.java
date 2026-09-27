package com.maggotkingtriptracker.tracking;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Reads all-time Maggot King records kept by RuneLite's core plugins for the logged-in account:
 * the kill count saved by Chat Commands and the loot saved by the Loot Tracker.
 */
@Slf4j
class AllTimeRecords
{
	private static final String LOOT_TRACKER_GROUP = "loottracker";
	private static final String KILL_COUNT_GROUP = "killcount";

	private final ConfigManager configManager;
	private final Gson gson;
	private String cachedJson;
	private LootTrackerRecord cachedRecord;

	AllTimeRecords(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/**
	 * @return the records, or null if the Loot Tracker has none for this boss on this account
	 */
	Snapshot read(String bossName)
	{
		String json = configManager.getRSProfileConfiguration(LOOT_TRACKER_GROUP, "drops_NPC_" + bossName);
		LootTrackerRecord record = parse(json);
		Integer killCount = record == null ? null
			: configManager.getRSProfileConfiguration(KILL_COUNT_GROUP, bossName.toLowerCase(), Integer.class);
		return snapshot(record, killCount);
	}

	static Snapshot snapshot(LootTrackerRecord record, Integer killCount)
	{
		if (record == null || record.kills <= 0)
		{
			return null;
		}

		Map<Integer, Integer> drops = new HashMap<>();
		if (record.drops != null)
		{
			for (int i = 0; i + 1 < record.drops.length; i += 2)
			{
				drops.merge(record.drops[i], record.drops[i + 1], Integer::sum);
			}
		}
		return new Snapshot(record.kills, killCount, record.first, drops);
	}

	private LootTrackerRecord parse(String json)
	{
		if (json == null)
		{
			return null;
		}
		if (json.equals(cachedJson))
		{
			return cachedRecord;
		}
		try
		{
			cachedRecord = gson.fromJson(json, LootTrackerRecord.class);
		}
		catch (JsonParseException e)
		{
			log.debug("Unreadable Loot Tracker record", e);
			cachedRecord = null;
		}
		cachedJson = json;
		return cachedRecord;
	}

	/**
	 * The fields of the Loot Tracker's saved record that are used here.
	 */
	static class LootTrackerRecord
	{
		int kills;
		long first;
		/**
		 * Item id and quantity pairs.
		 */
		int[] drops;
	}

	@Value
	public static class Snapshot
	{
		/**
		 * Kills recorded by the Loot Tracker (loot-dropping kills, i.e. Open-stomach).
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
}
