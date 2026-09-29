package com.maggotkingtriptracker.model;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * Charged items whose use is counted from attacks. The game's charge varbits are not updated live. Each is priced
 * from what recharges it: {@link #getComponents()} buy {@link #getChargesPerRecharge()} charges.
 */
@Getter
public enum ChargeType
{
	/**
	 * One charge per successful melee hit; a blood shard adds 10,000 charges.
	 */
	BLOOD_FURY(ItemID.BLOOD_AMULET, 10_000, new Component(ItemID.BLOOD_SHARD, 1)),
	/**
	 * One charge per fire spell; a burnt or searing page adds 20 charges. The page priced is chosen in config.
	 */
	TOME_OF_FIRE(ItemID.TOME_OF_FIRE, 20, new Component(ItemID.WINT_SEARING_PAGE, 1)),
	/**
	 * One revenant ether per shot (special attack included) for the revenant cave bows.
	 */
	WILDERNESS_WEAPON(ItemID.WILD_CAVE_WEBWEAVER_CHARGED, 1, new Component(ItemID.WILD_CAVE_SHARD, 1)),
	/**
	 * One charge per attack; 100 charges take a vial of blood and 300 blood runes (OSRS Wiki).
	 */
	SCYTHE_OF_VITUR(ItemID.SCYTHE_OF_VITUR, 100, new Component(ItemID.VIAL_BLOOD, 1), new Component(ItemID.BLOODRUNE, 300)),
	/**
	 * One charge per cast; each charge takes 2 soul runes and 5 chaos runes (OSRS Wiki).
	 */
	TUMEKENS_SHADOW(ItemID.TUMEKENS_SHADOW, 1, new Component(ItemID.SOULRUNE, 2), new Component(ItemID.CHAOSRUNE, 5));

	/**
	 * Some of an item that recharges a charged item.
	 */
	@Value
	public static class Component
	{
		int itemId;
		int quantity;
	}

	/**
	 * The charged item, used for the icon and name in the panel.
	 */
	private final int sourceItemId;
	/**
	 * Charges that {@link #getComponents()} buy.
	 */
	private final int chargesPerRecharge;
	/**
	 * What recharges it, e.g. a blood shard, or a vial of blood and blood runes. The first is the one saved on the
	 * supply line.
	 */
	private final List<Component> components;

	ChargeType(int sourceItemId, int chargesPerRecharge, Component... components)
	{
		this.sourceItemId = sourceItemId;
		this.chargesPerRecharge = chargesPerRecharge;
		this.components = ImmutableList.copyOf(components);
	}

	/**
	 * The first recharge item; for the Tome of fire, the default page (the config picks the one priced).
	 */
	public int getChargeItemId()
	{
		return components.get(0).getItemId();
	}

	/**
	 * @return the charge type whose charged item this is, or null
	 */
	public static ChargeType forSourceItem(int itemId)
	{
		for (ChargeType type : values())
		{
			if (type.sourceItemId == itemId)
			{
				return type;
			}
		}
		return null;
	}
}
