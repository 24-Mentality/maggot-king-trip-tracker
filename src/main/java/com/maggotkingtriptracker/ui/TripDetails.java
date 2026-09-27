package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.SupplyCategory;
import com.maggotkingtriptracker.view.TripView;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;

/**
 * Loot, supplies and dropped items for one trip, each as a loot-tracker style box.
 */
class TripDetails extends JPanel
{
	TripDetails(ItemManager itemManager, TripView trip)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);

		long activeMs = trip.activeMsAt(System.currentTimeMillis());
		long lootPerKill = trip.getKills() > 0 ? trip.getLootValue() / trip.getKills() : 0;
		List<String[]> lootStats = new ArrayList<>();
		lootStats.add(new String[]{"GP/Kill", UiFormat.gp(lootPerKill)});
		lootStats.add(new String[]{"Kills", String.valueOf(trip.getKills())});
		lootStats.add(new String[]{"GP/Hr", UiFormat.gp(TripMath.gpPerHour(trip.getLootValue(), activeMs))});
		lootStats.add(new String[]{"Total Gp", UiFormat.gp(trip.getLootValue())});
		add(section(new ItemSection(itemManager, "Loot", trip.getLoot(), "No loot yet", lootStats)));

		List<String[]> supplyStats = new ArrayList<>();
		supplyStats.add(new String[]{"Total", UiFormat.gp(trip.getSupplyCost())});
		for (SupplyCategory category : trip.getSupplyCategories())
		{
			supplyStats.add(new String[]{category.getName(), UiFormat.gp(category.getValue())});
		}
		add(section(new ItemSection(itemManager, "Supplies", trip.getSupplies(), "No supplies used yet", supplyStats)));

		if (!trip.getDropped().isEmpty())
		{
			List<String[]> droppedStats = new ArrayList<>();
			droppedStats.add(new String[]{"Total", UiFormat.gp(trip.getDroppedCost())});
			add(section(new ItemSection(itemManager, "Dropped", trip.getDropped(), null, droppedStats)));
		}
	}

	private static ItemSection section(ItemSection section)
	{
		section.setAlignmentX(LEFT_ALIGNMENT);
		return section;
	}
}
