package com.maggotkingtriptracker.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ChargeTypeTest
{
	@Test
	public void rechargesFromTheWiki()
	{
		ChargeType scythe = ChargeType.forSourceItem(ItemID.SCYTHE_OF_VITUR);
		assertSame(ChargeType.SCYTHE_OF_VITUR, scythe);
		assertEquals(100, scythe.getChargesPerRecharge());
		assertEquals(ItemID.VIAL_BLOOD, scythe.getChargeItemId());
		assertEquals(300, scythe.getComponents().get(1).getQuantity());

		ChargeType shadow = ChargeType.TUMEKENS_SHADOW;
		assertEquals(1, shadow.getChargesPerRecharge());
		assertEquals(ItemID.SOULRUNE, shadow.getComponents().get(0).getItemId());
		assertEquals(2, shadow.getComponents().get(0).getQuantity());
		assertEquals(ItemID.CHAOSRUNE, shadow.getComponents().get(1).getItemId());
		assertEquals(5, shadow.getComponents().get(1).getQuantity());

		assertNull(ChargeType.forSourceItem(ItemID.CRIMSON_KISTEN));
	}

	@Test
	public void chargeLinesArePricedFromTheWholeRecharge()
	{
		// 132 scythe attacks with a vial of blood at 900,000 and blood runes at 400: 100 charges cost 1,020,000
		ItemEntry scythe = ItemEntry.charges(ItemID.SCYTHE_OF_VITUR, 132, ItemID.VIAL_BLOOD, 900_000 + 300 * 400, 100);
		assertEquals(1_346_400, scythe.totalValue());

		// 135 shadow casts with soul runes at 150 and chaos runes at 80: 700 gp a charge
		ItemEntry shadow = ItemEntry.charges(ItemID.TUMEKENS_SHADOW, 135, ItemID.SOULRUNE, 2 * 150 + 5 * 80, 1);
		assertEquals(94_500, shadow.totalValue());
	}
}
