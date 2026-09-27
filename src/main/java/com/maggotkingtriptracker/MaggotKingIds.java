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

	/**
	 * Region just outside the lair: where the lair exit leads and where re-entries start.
	 */
	public static final int LAIR_ENTRANCE_REGION_ID = 10618;

	public static final int BOSS = NpcID.MAGGOT_KING;
	public static final int CORPSE = NpcID.MAGGOT_KING_CORPSE;

	/**
	 * The aranei scout variants that handle death recovery (grave moves) in Vampyrium.
	 */
	public static final Set<Integer> ARANEI_DEATH_HELPERS = ImmutableSet.of(
		NpcID.VAMPYRIUM_ARANEI_DEATH_HELPER,
		NpcID.VAMPYRIUM_ARANEI_DEATH_HELPER_1OP,
		NpcID.VAMPYRIUM_ARANEI_DEATH_HELPER_3OP
	);

	public static final Set<Integer> EGGS = ImmutableSet.of(
		ItemID.MAGGOT_EGG,
		ItemID.SICKLY_MAGGOT_EGG,
		ItemID.WARM_MAGGOT_EGG,
		ItemID.PULSATING_MAGGOT_EGG,
		ItemID.WRIGGLING_MAGGOT_EGG,
		ItemID.WRITHING_MAGGOT_EGG
	);

	/**
	 * Drops highlighted as uniques in the panel.
	 */
	public static final Set<Integer> UNIQUES = ImmutableSet.of(
		ItemID.ELDER_VENATOR_FANG,
		ItemID.CRIMSON_KISTEN,
		ItemID.MAGGOTKINGPET
	);

	public static final int PET_ITEM = ItemID.MAGGOTKINGPET;
	public static final int UNIQUES_FANG = ItemID.ELDER_VENATOR_FANG;
	public static final int UNIQUES_KISTEN = ItemID.CRIMSON_KISTEN;

	/**
	 * What the aranei scout accepts for moving a gravestone.
	 */
	public static final Set<Integer> GRAVE_MOVE_PAYMENTS = ImmutableSet.of(
		ItemID.COINS,
		ItemID.VIAL_BLOOD,
		ItemID.STYMPHIKE_FEATHER
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
