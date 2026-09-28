package com.maggotkingtriptracker.view;

import java.util.List;
import lombok.Value;

@Value
public class PanelState
{
	public enum Status
	{
		LOGGED_OUT,
		LOADING,
		IN_TRIP,
		/**
		 * Logged out mid-trip, within the grace period.
		 */
		PAUSED,
		/**
		 * Paused with the Pause button while in the lair.
		 */
		AFK_PAUSED,
		IDLE,
	}

	Status status;
	/**
	 * The trip in progress, or the most recent completed trip when idle; null if there are none.
	 */
	TripView currentTrip;
	/**
	 * Completed trips, newest first.
	 */
	List<TripView> history;
	LifetimeView lifetime;
	/**
	 * Null when no goal is set.
	 */
	GoalView goal;
	/**
	 * The Pause button is active (trip clock and/or goal clock stopped).
	 */
	boolean afkPaused;
	boolean readOnly;
}
