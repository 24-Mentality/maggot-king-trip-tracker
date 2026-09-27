package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.ItemView;
import com.maggotkingtriptracker.view.SupplyCategory;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Loot, supplies and dropped item grids for one trip.
 */
class TripDetails extends JPanel
{
	TripDetails(ItemManager itemManager, TripView trip)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);

		long now = System.currentTimeMillis();
		long activeMs = trip.activeMsAt(now);
		long lootPerKill = trip.getKills() > 0 ? trip.getLootValue() / trip.getKills() : 0;
		long lootPerHour = TripMath.gpPerHour(trip.getLootValue(), activeMs);
		List<String> lootStats = new ArrayList<>();
		lootStats.add("Kills: " + trip.getKills());
		lootStats.add("Loot/kill: " + UiFormat.gp(lootPerKill));
		lootStats.add("Loot/hr: " + UiFormat.gp(lootPerHour));
		lootStats.add("Net: " + UiFormat.gp(trip.getNetProfit()));
		add(section(itemManager, "Loot", trip.getLoot(), "No loot yet", lootStats));

		List<String> supplyStats = new ArrayList<>();
		for (SupplyCategory category : trip.getSupplyCategories())
		{
			supplyStats.add(category.getName() + ": " + UiFormat.gp(category.getValue()));
		}
		add(section(itemManager, "Supplies", trip.getSupplies(), "No supplies used yet", supplyStats));
		if (!trip.getDropped().isEmpty())
		{
			add(section(itemManager, "Dropped", trip.getDropped(), null, Collections.emptyList()));
		}
	}

	private static JComponent section(ItemManager itemManager, String title, List<ItemView> items, String emptyText,
		List<String> stats)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 3));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		panel.setAlignmentX(LEFT_ALIGNMENT);

		long total = 0;
		for (ItemView item : items)
		{
			total += item.getTotalValue();
		}

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(titleLabel, BorderLayout.WEST);
		if (!items.isEmpty())
		{
			JLabel totalLabel = new JLabel(UiFormat.gp(total));
			totalLabel.setFont(FontManager.getRunescapeSmallFont());
			totalLabel.setForeground(UiFormat.MUTED_TEXT);
			totalLabel.setToolTipText(UiFormat.fullGp(total));
			header.add(totalLabel, BorderLayout.EAST);
		}
		JPanel top = new JPanel(new BorderLayout(0, 2));
		top.setOpaque(false);
		top.add(header, BorderLayout.NORTH);
		if (!stats.isEmpty())
		{
			// Two short stats per line under the section title
			JPanel statGrid = new JPanel(new GridLayout(0, 2, 6, 0));
			statGrid.setOpaque(false);
			for (String stat : stats)
			{
				JLabel label = new JLabel(stat);
				label.setFont(FontManager.getRunescapeSmallFont());
				label.setForeground(UiFormat.MUTED_TEXT);
				statGrid.add(label);
			}
			top.add(statGrid, BorderLayout.CENTER);
		}
		panel.add(top, BorderLayout.NORTH);

		panel.add(items.isEmpty() ? ItemGrid.emptyMessage(emptyText) : new ItemGrid(itemManager, items), BorderLayout.CENTER);
		return panel;
	}
}
