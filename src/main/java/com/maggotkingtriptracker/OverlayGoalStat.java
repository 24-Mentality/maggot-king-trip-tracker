package com.maggotkingtriptracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The overlay's first row: kill goal progress. Hidden while no goal is set.
 */
@Getter
@RequiredArgsConstructor
public enum OverlayGoalStat
{
	KILLS_PER_HOUR("Kills per hour (KPH)"),
	TIME_TO_GOAL("Time to goal (TTG)"),
	KILLS_DONE("Kills done"),
	KILLS_LEFT("Kills left"),
	NONE("Nothing");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
