package com.maggotkingtriptracker.boss;

import lombok.Value;

/**
 * Where RuneLite's core plugins keep all-time records for a boss in the RS profile config.
 */
@Value
public class AllTimeSource
{
	/**
	 * Key in the Loot Tracker's "loottracker" group, e.g. "drops_NPC_Maggot King".
	 */
	String lootTrackerKey;
	/**
	 * Key in Chat Commands' "killcount" group, e.g. "maggot king"; null if none.
	 */
	String killCountKey;
	/**
	 * The variant these records belong to, or null for all of the boss.
	 */
	String variant;
}
