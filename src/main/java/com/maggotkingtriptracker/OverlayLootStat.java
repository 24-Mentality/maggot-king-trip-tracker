package com.maggotkingtriptracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The overlay's third row: the trip's profit.
 */
@Getter
@RequiredArgsConstructor
public enum OverlayLootStat
{
	NET_PROFIT("Net profit"),
	NET_GP_PER_HOUR("Net GP/hr"),
	NONE("Nothing");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
