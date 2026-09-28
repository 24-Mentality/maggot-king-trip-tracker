package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripEndReason;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
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
	private final GoalCard goalCard;
	private final JLabel status = new JLabel();
	private final TripSummaryCard summary = new TripSummaryCard();
	private final LuckCard luckCard = new LuckCard();
	private final DropChancesCard dropChances;
	private final JPanel luckHolder = new JPanel(new BorderLayout());
	private final JPanel detailsHolder = new JPanel();
	private boolean live;
	private TripView shownDetails;

	CurrentTripPanel(ItemManager itemManager, Runnable onSetGoal, Runnable onPause, Runnable onResetGoal)
	{
		this.itemManager = itemManager;
		this.goalCard = new GoalCard(itemManager, onSetGoal, onPause, onResetGoal);
		this.dropChances = new DropChancesCard(itemManager);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(UiFormat.MUTED_TEXT);
		status.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
		status.setAlignmentX(LEFT_ALIGNMENT);
		summary.setAlignmentX(LEFT_ALIGNMENT);
		detailsHolder.setLayout(new BoxLayout(detailsHolder, BoxLayout.Y_AXIS));
		detailsHolder.setOpaque(false);
		detailsHolder.setAlignmentX(LEFT_ALIGNMENT);

		goalCard.setAlignmentX(LEFT_ALIGNMENT);
		JPanel goalSpacer = new JPanel();
		goalSpacer.setOpaque(false);
		goalSpacer.setAlignmentX(LEFT_ALIGNMENT);
		goalSpacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

		add(goalCard);
		add(goalSpacer);
		add(status);
		add(summary);
		luckHolder.setOpaque(false);
		luckHolder.setAlignmentX(LEFT_ALIGNMENT);
		luckHolder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		luckHolder.add(luckCard, BorderLayout.CENTER);
		JPanel dropHolder = new JPanel(new BorderLayout());
		dropHolder.setOpaque(false);
		dropHolder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		dropHolder.add(dropChances, BorderLayout.CENTER);
		luckHolder.add(dropHolder, BorderLayout.SOUTH);
		add(luckHolder);
		add(detailsHolder);
	}

	void update(PanelState state, long now)
	{
		TripView trip = state.getCurrentTrip();
		live = state.getStatus() == PanelState.Status.IN_TRIP;
		goalCard.setVisible(state.getLifetime() != null);
		goalCard.setPauseState(state.isPausedInLair(), state.isCanPause());
		goalCard.setGoal(state.getGoal(), now);
		summary.setPaused(state.getPauseText() != null);
		luckHolder.setVisible(state.getLifetime() != null);
		if (state.getLifetime() != null)
		{
			luckCard.update(state.getLifetime().getDryness());
			dropChances.update(state.getLifetime().getDryness());
		}
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
		goalCard.tick(now);
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
		return a.getKills() == b.getKills()
			&& a.getLoot().equals(b.getLoot())
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
			case AFK_PAUSED:
				return state.getPauseText();
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
