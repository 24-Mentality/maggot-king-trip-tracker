package com.maggotkingtriptracker;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * Drop rates from the OSRS Wiki, used for the dryness and pet chance numbers.
 * Uniques and the pet only come from Open-stomach.
 */
public final class MaggotKingRates
{
	public static final double ANY_UNIQUE = 1 / 205.6;

	/**
	 * Unique item to its rate per Open-stomach kill.
	 */
	public static final Map<Integer, Double> UNIQUES = ImmutableMap.of(
		ItemID.ELDER_VENATOR_FANG, 1 / 340.0,
		ItemID.CRIMSON_KISTEN, 1 / 520.0
	);

	public static final double PET_PER_STOMACH = 1 / 3500.0;

	/**
	 * Egg tier to its pet chance when popped, lowest tier first.
	 */
	public static final Map<Integer, Double> EGG_PET = ImmutableMap.<Integer, Double>builder()
		.put(ItemID.MAGGOT_EGG, 1 / 3000.0)
		.put(ItemID.SICKLY_MAGGOT_EGG, 1 / 2500.0)
		.put(ItemID.WARM_MAGGOT_EGG, 1 / 1250.0)
		.put(ItemID.PULSATING_MAGGOT_EGG, 1 / 500.0)
		.put(ItemID.WRIGGLING_MAGGOT_EGG, 1 / 10.0)
		.put(ItemID.WRITHING_MAGGOT_EGG, 1 / 2.0)
		.build();

	private MaggotKingRates()
	{
	}
}
