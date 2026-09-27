package com.maggotkingtriptracker.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Charged items whose use is counted from attacks. The game's charge varbits are not updated live.
 */
@Getter
@RequiredArgsConstructor
public enum ChargeType
{
	/**
	 * One charge per successful melee hit; a blood shard adds 10,000 charges.
	 */
	BLOOD_FURY(ItemID.BLOOD_AMULET, ItemID.BLOOD_SHARD, 10_000),
	/**
	 * One charge per fire spell; a burnt or searing page adds 20 charges. The page priced is chosen in config.
	 */
	TOME_OF_FIRE(ItemID.TOME_OF_FIRE, ItemID.WINT_SEARING_PAGE, 20),
	/**
	 * One revenant ether per shot (special attack included) for the revenant cave bows.
	 */
	WILDERNESS_WEAPON(ItemID.WILD_CAVE_WEBWEAVER_CHARGED, ItemID.WILD_CAVE_SHARD, 1);

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
