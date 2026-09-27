package com.maggotkingtriptracker.view;

import lombok.Value;

@Value
public class GoalView
{
	int target;
	int done;
	long activeMs;
	/**
	 * When activeMs was read, so the panel can keep the clock running while logged in.
	 */
	long asOf;
	boolean running;

	public long activeMsAt(long now)
	{
		return activeMs + (running ? Math.max(0, now - asOf) : 0);
	}
}
