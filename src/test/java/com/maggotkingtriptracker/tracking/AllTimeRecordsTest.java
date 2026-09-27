package com.maggotkingtriptracker.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class AllTimeRecordsTest
{
	@Test
	public void parsesLootTrackerRecord()
	{
		// Same shape as RuneLite's saved loottracker drops_NPC_Maggot King value
		String json = "{\"type\":\"NPC\",\"name\":\"Maggot King\",\"kills\":2630,\"first\":1785447588633,"
			+ "\"last\":1790544734203,\"drops\":[2971,9801,33634,6,33631,5,450,5443]}";
		AllTimeRecords.LootTrackerRecord record = new Gson().fromJson(json, AllTimeRecords.LootTrackerRecord.class);
		AllTimeRecords.Snapshot snapshot = AllTimeRecords.snapshot(record, 2636);

		assertEquals(2630, snapshot.getLootKills());
		assertEquals(Integer.valueOf(2636), snapshot.getKillCount());
		assertEquals(6, snapshot.dropped(ItemID.ELDER_VENATOR_FANG));
		assertEquals(5, snapshot.dropped(ItemID.CRIMSON_KISTEN));
		assertEquals(0, snapshot.dropped(ItemID.MAGGOTKINGPET));
	}

	@Test
	public void noRecordMeansNoAllTimeData()
	{
		assertNull(AllTimeRecords.snapshot(null, 100));
	}
}
