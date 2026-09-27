package com.maggotkingtriptracker.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Charged items whose charges are read from the game's charge varbits.
 */
@Getter
@RequiredArgsConstructor
public enum ChargeType
{
	/**
	 * One charge per successful melee hit; a blood shard adds 10,000 charges.
	 */
	BLOOD_FURY(VarbitID.CHARGES_BLOOD_FURY_QUANTITY, ItemID.BLOOD_AMULET, ItemID.BLOOD_SHARD, 10_000),
	/**
	 * One charge per fire spell; a burnt or searing page adds 20 charges. The page priced is chosen in config.
	 */
	TOME_OF_FIRE(VarbitID.CHARGES_TOME_OF_FIRE_QUANTITY, ItemID.TOME_OF_FIRE, ItemID.WINT_SEARING_PAGE, 20),
	/**
	 * One revenant ether per shot. The varbit is shared by all revenant cave weapons.
	 */
	WILDERNESS_WEAPON(VarbitID.CHARGES_WILDERNESS_WEAPON_QUANTITY, ItemID.WILD_CAVE_WEBWEAVER_CHARGED, ItemID.WILD_CAVE_SHARD, 1);

	private final int varbit;
	/**
	 * The charged item, used for the icon and name in the panel.
	 */
	private final int sourceItemId;
	/**
	 * What recharges it: blood shard, tome page or revenant ether.
	 */
	private final int chargeItemId;
	private final int chargesPerItem;
}
