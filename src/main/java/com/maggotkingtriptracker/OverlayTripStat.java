package com.maggotkingtriptracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The overlay's second row: the trip's times.
 */
@Getter
@RequiredArgsConstructor
public enum OverlayTripStat
{
	CURRENT_KILL("Current kill"),
	TRIP_TIME("Trip time"),
	KILLS("Trip kills"),
	AVERAGE_KILL("Average kill"),
	PB("PB (fastest this trip)"),
	NONE("Nothing");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
