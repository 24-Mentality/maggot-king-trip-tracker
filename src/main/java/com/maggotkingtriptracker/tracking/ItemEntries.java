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
			if (entry.getItemId() == itemId && entry.isPerDose() == perDose && !entry.isPending())
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
		merge(entries, added.getItemId(), added.getQuantity(), added.getPriceEach(), added.isPerDose());
	}
}
