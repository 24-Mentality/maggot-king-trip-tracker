package com.maggotkingtriptracker.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Everything stored for one account. Serialized with Gson to one JSON file per account.
 */
@Data
public class AccountHistory
{
	public static final int CURRENT_SCHEMA_VERSION = 1;

	private int schemaVersion = CURRENT_SCHEMA_VERSION;
	private long accountHash;
	private String lastDisplayName;
	private List<Trip> trips = new ArrayList<>();
	/**
	 * Every maggot egg popped, anywhere.
	 */
	private List<EggPop> eggPops = new ArrayList<>();
	/**
	 * Tarnished item id to (polished result id to count), for every polish seen.
	 */
	private Map<Integer, Map<Integer, Integer>> polishOutcomes = new HashMap<>();
}
