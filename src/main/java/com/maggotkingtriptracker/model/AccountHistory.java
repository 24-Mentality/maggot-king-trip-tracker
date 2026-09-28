package com.maggotkingtriptracker.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * Everything stored for one account. Serialized with Gson to one JSON file per account.
 * Schema 1 kept the Maggot King's data at the top level; schema 2 keeps each boss's data under its id
 * (see HistoryMigrator).
 */
@Data
public class AccountHistory
{
	public static final int CURRENT_SCHEMA_VERSION = 2;

	private int schemaVersion = CURRENT_SCHEMA_VERSION;
	private long accountHash;
	private String lastDisplayName;
	/**
	 * Boss id (BossDefinition#getId) to that boss's data.
	 */
	private Map<String, BossHistory> bosses = new LinkedHashMap<>();

	/**
	 * @return this boss's data, created empty if there is none yet
	 */
	public BossHistory boss(String bossId)
	{
		return bosses.computeIfAbsent(bossId, id -> new BossHistory());
	}
}
