package com.maggotkingtriptracker.tracking;

import static org.junit.Assert.assertEquals;
import com.maggotkingtriptracker.model.ItemEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ItemEntriesTest
{
	@Test
	public void mergesSameItemWithWeightedPrice()
	{
		List<ItemEntry> entries = new ArrayList<>();
		ItemEntries.merge(entries, 13441, 2, 1_000, false);
		ItemEntries.merge(entries, 13441, 2, 1_200, false);

		assertEquals(1, entries.size());
		assertEquals(4, entries.get(0).getQuantity());
		assertEquals(1_100, entries.get(0).getPriceEach());
	}

	@Test
	public void keepsDoseAndItemLinesSeparate()
	{
		List<ItemEntry> entries = new ArrayList<>();
		ItemEntries.merge(entries, 2434, 3, 2_000, true);
		ItemEntries.merge(entries, 2434, 1, 8_000, false);

		assertEquals(2, entries.size());
	}

	@Test
	public void ignoresZeroQuantity()
	{
		List<ItemEntry> entries = new ArrayList<>();
		ItemEntries.merge(entries, 2434, 0, 2_000, true);
		assertEquals(0, entries.size());
	}
}
