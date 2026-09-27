package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.view.TripView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class HistoryPanel extends JPanel
{
	private final ItemManager itemManager;
	private final Consumer<TripView> onDelete;
	private final Set<String> expandedTrips = new HashSet<>();
	private List<TripView> shown;

	HistoryPanel(ItemManager itemManager, Consumer<TripView> onDelete)
	{
		this.itemManager = itemManager;
		this.onDelete = onDelete;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
	}

	void update(List<TripView> trips)
	{
		if (trips == shown)
		{
			return;
		}
		shown = trips;

		removeAll();
		JLabel count = new JLabel(trips.isEmpty() ? "No completed trips yet."
			: trips.size() + (trips.size() == 1 ? " trip" : " trips") + " · right-click a trip to delete it");
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(UiFormat.MUTED_TEXT);
		count.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		count.setAlignmentX(LEFT_ALIGNMENT);
		add(count);

		for (TripView trip : trips)
		{
			TripCard card = new TripCard(itemManager, trip, expandedTrips.contains(trip.getId()), this::toggled, onDelete);
			card.setAlignmentX(LEFT_ALIGNMENT);
			add(card);
		}
		revalidate();
		repaint();
	}

	private void toggled(TripCard card)
	{
		if (card.isExpanded())
		{
			expandedTrips.add(card.getTripId());
		}
		else
		{
			expandedTrips.remove(card.getTripId());
		}
	}
}
