package com.maggotkingtriptracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What the goal overlay can show.
 */
@Getter
@RequiredArgsConstructor
public enum GoalOverlayLine
{
	KILLS_PER_HOUR("Kills per hour"),
	KILLS_DONE("Kills done"),
	KILLS_LEFT("Kills left"),
	TIME_TO_GOAL("Time to goal"),
	PROGRESS_BAR("Progress bar");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
