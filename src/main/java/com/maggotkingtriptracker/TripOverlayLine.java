package com.maggotkingtriptracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What the trip overlay can show: the Trip tab's time row.
 */
@Getter
@RequiredArgsConstructor
public enum TripOverlayLine
{
	TIME("Trip time"),
	KILLS("Kills"),
	AVERAGE_KILL("Average kill"),
	PB("PB (fastest this trip)"),
	CURRENT_KILL("Current kill");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
