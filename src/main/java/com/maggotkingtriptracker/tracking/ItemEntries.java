package com.maggotkingtriptracker.tracking;

import com.maggotkingtriptracker.model.ItemEntry;
import java.util.List;

final class ItemEntries
{
	private ItemEntries()
	{
	}

	/**
	 * Adds to an existing line for the same item, keeping a quantity-weighted recorded price.
	 */
	static void merge(List<ItemEntry> entries, int itemId, long quantity, long priceEach, boolean perDose)
	{
		if (quantity <= 0)
		{
			return;
		}

		for (ItemEntry entry : entries)
		{
			if (entry.getItemId() == itemId && entry.isPerDose() == perDose && !entry.isPending() && !entry.isCharges()
				&& entry.getPolishedFrom() == 0)
			{
				long total = entry.getQuantity() + quantity;
				double value = (double) entry.getQuantity() * entry.getPriceEach() + (double) quantity * priceEach;
				entry.setQuantity(total);
				entry.setPriceEach(Math.round(value / total));
				return;
			}
		}

		ItemEntry entry = new ItemEntry(itemId, quantity, priceEach);
		entry.setPerDose(perDose);
		entries.add(entry);
	}

	static void merge(List<ItemEntry> entries, ItemEntry added)
	{
		if (!added.isCharges())
		{
			merge(entries, added.getItemId(), added.getQuantity(), added.getPriceEach(), added.isPerDose());
			return;
		}
		if (added.getQuantity() <= 0)
		{
			return;
		}

		for (ItemEntry entry : entries)
		{
			// Blowpipe dart lines for different capes share an item and dart but not the darts lost per 25 shots
			if (entry.getItemId() == added.getItemId() && entry.getChargeItemId() == added.getChargeItemId()
				&& entry.getChargesPerItem() == added.getChargesPerItem())
			{
				long total = entry.getQuantity() + added.getQuantity();
				double value = (double) entry.getQuantity() * entry.getPriceEach() + (double) added.getQuantity() * added.getPriceEach();
				entry.setQuantity(total);
				entry.setPriceEach(Math.round(value / total));
				return;
			}
		}
		entries.add(ItemEntry.charges(added.getItemId(), added.getQuantity(), added.getChargeItemId(),
			added.getPriceEach(), added.getChargesPerItem()));
	}
}
