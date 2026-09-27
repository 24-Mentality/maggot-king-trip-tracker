package com.maggotkingtriptracker.view;

import lombok.Value;

@Value
public class ItemView
{
	int itemId;
	String name;
	long quantity;
	long totalValue;
	boolean perDose;
	boolean unique;
	boolean pending;
	/**
	 * For charge lines, the name of what recharges the item (e.g. "Blood shard"); otherwise null.
	 */
	String chargeItemName;
	int chargesPerItem;

	public boolean isCharges()
	{
		return chargeItemName != null;
	}
}
