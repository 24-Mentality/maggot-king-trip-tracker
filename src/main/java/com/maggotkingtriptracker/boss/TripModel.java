package com.maggotkingtriptracker.boss;

/**
 * How a boss's trips are shaped.
 */
public enum TripModel
{
	/**
	 * Several kills inside an area or instance per trip, from entering it until leaving (Maggot King, Nightmare).
	 */
	INSTANCE_KILLS,
	/**
	 * One raid per trip, from entering until the raid ends (Theatre of Blood). Not implemented yet.
	 */
	ONE_RAID,
}
