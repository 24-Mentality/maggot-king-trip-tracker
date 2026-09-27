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
		PAUSED,
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
	boolean readOnly;
}
