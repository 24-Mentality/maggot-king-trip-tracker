package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.view.ItemView;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
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

		add(section(itemManager, "Loot", trip.getLoot(), "No loot yet"));
		add(section(itemManager, "Supplies", trip.getSupplies(), "No supplies used yet"));
		if (!trip.getDropped().isEmpty())
		{
			add(section(itemManager, "Dropped", trip.getDropped(), null));
		}
	}

	private static JComponent section(ItemManager itemManager, String title, List<ItemView> items, String emptyText)
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
		panel.add(header, BorderLayout.NORTH);

		panel.add(items.isEmpty() ? ItemGrid.emptyMessage(emptyText) : new ItemGrid(itemManager, items), BorderLayout.CENTER);
		return panel;
	}
}
