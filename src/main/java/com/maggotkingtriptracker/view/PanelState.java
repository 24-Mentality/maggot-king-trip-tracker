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
		 * Paused in the lair (Pause button or idle), or waiting just outside it.
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
	 * Why the open trip's clock is stopped, e.g. "Trip paused (idle)"; null while it runs.
	 */
	String pauseText;
	/**
	 * The clock is paused in the lair, so the Pause button reads Resume.
	 */
	boolean pausedInLair;
	/**
	 * Pausing is possible: in the lair on an open trip.
	 */
	boolean canPause;
	boolean readOnly;
}
