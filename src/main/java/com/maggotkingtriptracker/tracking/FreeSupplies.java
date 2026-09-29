package com.maggotkingtriptracker.tracking;

import com.maggotkingtriptracker.model.ItemEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Supplies obtained inside a raid (Theatre of Blood supply chest purchases, items picked up) cost nothing: only use
 * beyond what was obtained there is paid for. Counted per item, and per dose for potions, so a brew bought from the
 * chest is free whether it's drunk, dropped or kept. Charges are never covered.
 */
class FreeSupplies
{
	private final Map<Key, Long> allowance = new HashMap<>();

	/**
	 * Something obtained inside, as a supply line would count it (per dose for potions).
	 */
	void acquired(ItemEntry entry)
	{
		if (!entry.isCharges() && entry.getQuantity() > 0)
		{
			allowance.merge(Key.of(entry), entry.getQuantity(), Long::sum);
		}
	}

	/**
	 * @param used supply lines about to be recorded
	 * @return the part of them that was brought in and so is paid for; lines fully covered are left out. The
	 * allowance used up is removed.
	 */
	List<ItemEntry> paidFor(List<ItemEntry> used)
	{
		List<ItemEntry> paid = new ArrayList<>();
		for (ItemEntry entry : used)
		{
			if (entry.isCharges())
			{
				paid.add(entry);
				continue;
			}
			Key key = Key.of(entry);
			long free = allowance.getOrDefault(key, 0L);
			long covered = Math.min(free, entry.getQuantity());
			if (covered > 0)
			{
				if (free - covered > 0)
				{
					allowance.put(key, free - covered);
				}
				else
				{
					allowance.remove(key);
				}
			}
			long rest = entry.getQuantity() - covered;
			if (rest > 0)
			{
				ItemEntry part = new ItemEntry(entry.getItemId(), rest, entry.getPriceEach());
				part.setPerDose(entry.isPerDose());
				paid.add(part);
			}
		}
		return paid;
	}

	/**
	 * @return how much of this item (doses for potions) is still free
	 */
	long remaining(int itemId, boolean perDose)
	{
		return allowance.getOrDefault(new Key(itemId, perDose), 0L);
	}

	void clear()
	{
		allowance.clear();
	}

	private static final class Key
	{
		final int itemId;
		final boolean perDose;

		Key(int itemId, boolean perDose)
		{
			this.itemId = itemId;
			this.perDose = perDose;
		}

		static Key of(ItemEntry entry)
		{
			return new Key(entry.getItemId(), entry.isPerDose());
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Key))
			{
				return false;
			}
			Key other = (Key) o;
			return itemId == other.itemId && perDose == other.perDose;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(itemId, perDose);
		}
	}
}
