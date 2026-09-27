package com.maggotkingtriptracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An item and quantity with the GE price recorded when it was gained or used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemEntry
{
	private int itemId;
	private long quantity;
	/**
	 * GE price per unit at record time. For per-dose entries this is the price of one dose.
	 */
	private long priceEach;
	/**
	 * Quantity counts potion doses (itemId is the highest-dose variant), not whole items.
	 */
	private boolean perDose;
	/**
	 * Tarnished drop whose real value is only known once it is polished.
	 */
	private boolean pending;
	private String pendingId;

	public ItemEntry(int itemId, long quantity, long priceEach)
	{
		this(itemId, quantity, priceEach, false, false, null);
	}

	public long totalValue()
	{
		return pending ? 0 : quantity * priceEach;
	}
}
