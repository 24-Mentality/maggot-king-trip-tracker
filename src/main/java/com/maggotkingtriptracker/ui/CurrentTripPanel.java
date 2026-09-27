package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripEndReason;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class CurrentTripPanel extends JPanel
{
	private final ItemManager itemManager;
	private final JLabel status = new JLabel();
	private final TripSummaryCard summary = new TripSummaryCard();
	private final JPanel detailsHolder = new JPanel();
	private boolean live;
	private TripView shownDetails;

	CurrentTripPanel(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(UiFormat.MUTED_TEXT);
		status.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		status.setAlignmentX(LEFT_ALIGNMENT);
		summary.setAlignmentX(LEFT_ALIGNMENT);
		detailsHolder.setLayout(new BoxLayout(detailsHolder, BoxLayout.Y_AXIS));
		detailsHolder.setOpaque(false);
		detailsHolder.setAlignmentX(LEFT_ALIGNMENT);

		add(status);
		add(summary);
		add(detailsHolder);
	}

	void update(PanelState state, long now)
	{
		TripView trip = state.getCurrentTrip();
		live = state.getStatus() == PanelState.Status.IN_TRIP;
		status.setText(statusText(state));

		summary.setVisible(trip != null);
		if (trip != null)
		{
			summary.setTrip(trip, now);
		}

		// Rebuilding icon grids resets hovered tooltips, so only do it when the items changed
		if (!sameItems(trip, shownDetails))
		{
			shownDetails = trip;
			detailsHolder.removeAll();
			if (trip != null)
			{
				detailsHolder.add(new TripDetails(itemManager, trip));
			}
		}
		revalidate();
		repaint();
	}

	void tick(long now)
	{
		if (live)
		{
			summary.tick(now);
		}
	}

	private static boolean sameItems(TripView a, TripView b)
	{
		if (a == null || b == null)
		{
			return a == b;
		}
		return a.getLoot().equals(b.getLoot())
			&& a.getSupplies().equals(b.getSupplies())
			&& a.getDropped().equals(b.getDropped());
	}

	private static String statusText(PanelState state)
	{
		switch (state.getStatus())
		{
			case LOGGED_OUT:
				return "Log in to track your trips.";
			case LOADING:
				return "Loading history...";
			case IN_TRIP:
				return "Trip in progress";
			case PAUSED:
				return "Trip paused (logged out)";
			default:
				TripView last = state.getCurrentTrip();
				if (last == null)
				{
					return "No trips yet. Enter the Maggot King's lair to start one.";
				}
				return "Last trip · " + UiFormat.dateTime(last.getStartedAt()) + " · " + endReason(last.getEndReason());
		}
	}

	static String endReason(TripEndReason reason)
	{
		if (reason == null)
		{
			return "in progress";
		}
		switch (reason)
		{
			case WALKED_OUT:
				return "walked out";
			case TELEPORT:
				return "teleported";
			case DEATH:
				return "died";
			case LOGOUT:
				return "logged out";
			default:
				return reason.name().toLowerCase();
		}
	}
}
