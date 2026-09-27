package com.maggotkingtriptracker;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/**
 * Game identifiers for the Maggot King encounter. Everything that has a gameval constant uses it.
 */
public final class MaggotKingIds
{
	/**
	 * Template (non-instanced) region of the Maggot King lair. There is no gameval for regions.
	 * Resolve the player's location with WorldPoint.fromLocalInstance before comparing.
	 */
	public static final int LAIR_REGION_ID = 11645;

	public static final int BOSS = NpcID.MAGGOT_KING;
	public static final int CORPSE = NpcID.MAGGOT_KING_CORPSE;

	public static final Set<Integer> EGGS = ImmutableSet.of(
		ItemID.MAGGOT_EGG,
		ItemID.SICKLY_MAGGOT_EGG,
		ItemID.WARM_MAGGOT_EGG,
		ItemID.PULSATING_MAGGOT_EGG,
		ItemID.WRIGGLING_MAGGOT_EGG,
		ItemID.WRITHING_MAGGOT_EGG
	);

	public static final Set<Integer> TARNISHED_ITEMS = ImmutableSet.of(
		ItemID.TARNISHED_LONGSWORD,
		ItemID.TARNISHED_SPEAR,
		ItemID.TARNISHED_2H_SWORD,
		ItemID.TARNISHED_BATTLEAXE,
		ItemID.TARNISHED_HALBERD,
		ItemID.TARNISHED_RING,
		ItemID.TARNISHED_BRACELET,
		ItemID.TARNISHED_NECKLACE,
		ItemID.TARNISHED_AMULET
	);

	private MaggotKingIds()
	{
	}
}
