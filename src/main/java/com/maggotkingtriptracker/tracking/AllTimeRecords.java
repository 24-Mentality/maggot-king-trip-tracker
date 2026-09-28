package com.maggotkingtriptracker.tracking;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.maggotkingtriptracker.boss.AllTimeSource;
import com.maggotkingtriptracker.model.AllTimeCounts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Reads all-time boss records kept by RuneLite's core plugins for the logged-in account:
 * the kill count saved by Chat Commands and the loot saved by the Loot Tracker.
 */
@Slf4j
class AllTimeRecords
{
	static final String LOOT_TRACKER_GROUP = "loottracker";
	static final String KILL_COUNT_GROUP = "killcount";

	private final ConfigManager configManager;
	private final Gson gson;
	/**
	 * Loot Tracker key to the last JSON read and its parsed record, so unchanged records aren't parsed again.
	 */
	private final Map<String, Cached> cache = new HashMap<>();

	AllTimeRecords(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/**
	 * Combines the records of every source for this variant (or all of them when {@code variant} is null).
	 *
	 * @return the records, or null if the Loot Tracker has none for this boss on this account
	 */
	AllTimeCounts read(List<AllTimeSource> sources, String variant)
	{
		AllTimeCounts combined = null;
		for (AllTimeSource source : sources)
		{
			if (variant != null && source.getVariant() != null && !variant.equals(source.getVariant()))
			{
				continue;
			}
			LootTrackerRecord record = parse(source.getLootTrackerKey(),
				configManager.getRSProfileConfiguration(LOOT_TRACKER_GROUP, source.getLootTrackerKey()));
			Integer killCount = record == null || source.getKillCountKey() == null ? null
				: configManager.getRSProfileConfiguration(KILL_COUNT_GROUP, source.getKillCountKey(), Integer.class);
			combined = combine(combined, snapshot(record, killCount));
		}
		return combined;
	}

	static AllTimeCounts combine(AllTimeCounts a, AllTimeCounts b)
	{
		if (a == null || b == null)
		{
			return a == null ? b : a;
		}
		Map<Integer, Integer> drops = new HashMap<>(a.getDrops());
		b.getDrops().forEach((id, quantity) -> drops.merge(id, quantity, Integer::sum));
		Integer killCount = a.getKillCount() == null ? b.getKillCount()
			: b.getKillCount() == null ? a.getKillCount() : Integer.valueOf(a.getKillCount() + b.getKillCount());
		return new AllTimeCounts(a.getLootKills() + b.getLootKills(), killCount,
			Math.min(a.getFirstRecordedAt(), b.getFirstRecordedAt()),
			Math.min(a.getLastRecordedAt(), b.getLastRecordedAt()), drops);
	}

	static AllTimeCounts snapshot(LootTrackerRecord record, Integer killCount)
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
		return new AllTimeCounts(record.kills, killCount, record.first, record.last, drops);
	}

	private LootTrackerRecord parse(String key, String json)
	{
		if (json == null)
		{
			return null;
		}
		Cached cached = cache.get(key);
		if (cached != null && json.equals(cached.json))
		{
			return cached.record;
		}
		LootTrackerRecord record;
		try
		{
			record = gson.fromJson(json, LootTrackerRecord.class);
		}
		catch (JsonParseException e)
		{
			log.debug("Unreadable Loot Tracker record", e);
			record = null;
		}
		cache.put(key, new Cached(json, record));
		return record;
	}

	@AllArgsConstructor
	private static class Cached
	{
		final String json;
		final LootTrackerRecord record;
	}

	/**
	 * The fields of the Loot Tracker's saved record that are used here.
	 */
	static class LootTrackerRecord
	{
		int kills;
		long first;
		long last;
		/**
		 * Item id and quantity pairs.
		 */
		int[] drops;
	}
}
