package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * Side panel with Trip, History and Lifetime tabs pinned at the top and the selected tab scrolling below.
 * All methods run on the Swing thread.
 */
public class TrackerPanel extends PluginPanel
{
	private final CurrentTripPanel currentTab;
	private final HistoryPanel historyTab;
	private final LifetimePanel lifetimeTab;
	private final JLabel readOnlyWarning = new JLabel();
	private final Timer timer;

	public TrackerPanel(ItemManager itemManager, PanelActions actions)
	{
		// Not wrapped in PluginPanel's scroll pane, so the tabs stay visible while content scrolls
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		currentTab = new CurrentTripPanel(itemManager, () -> promptGoal(actions), () -> confirmResetGoal(actions));
		historyTab = new HistoryPanel(itemManager, trip -> confirmDelete(trip, actions::deleteTrip));
		lifetimeTab = new LifetimePanel(actions, () -> confirmClear(actions::clearHistory));

		ScrollableContent display = new ScrollableContent();
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		display.setBorder(BorderFactory.createEmptyBorder(4, 5, 5, 5));

		MaterialTabGroup tabs = new MaterialTabGroup(display);
		// The default wrapping row hides the third tab at sidebar width; equal columns always fit
		tabs.setLayout(new GridLayout(1, 0));
		tabs.setBorder(BorderFactory.createEmptyBorder(6, 2, 2, 2));
		MaterialTab current = new MaterialTab("Trip", tabs, currentTab);
		MaterialTab history = new MaterialTab("History", tabs, historyTab);
		MaterialTab lifetime = new MaterialTab("Lifetime", tabs, lifetimeTab);
		for (MaterialTab tab : new MaterialTab[]{current, history, lifetime})
		{
			tab.setHorizontalAlignment(SwingConstants.CENTER);
			tabs.addTab(tab);
		}
		tabs.select(current);

		readOnlyWarning.setFont(FontManager.getRunescapeSmallFont());
		readOnlyWarning.setForeground(UiFormat.LOSS);
		readOnlyWarning.setText("<html>History could not be loaded or is from a newer version. Changes will not be saved.</html>");
		readOnlyWarning.setVisible(false);
		readOnlyWarning.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(tabs, BorderLayout.NORTH);
		north.add(readOnlyWarning, BorderLayout.SOUTH);

		JScrollPane scroll = new JScrollPane(display);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(north, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		timer = new Timer(1000, e -> currentTab.tick(System.currentTimeMillis()));
		timer.start();
	}

	public void update(PanelState state)
	{
		readOnlyWarning.setVisible(state.isReadOnly());
		currentTab.update(state, System.currentTimeMillis());
		historyTab.update(state.getHistory());
		lifetimeTab.update(state.getLifetime(), state.isReadOnly());
	}

	public void shutDown()
	{
		timer.stop();
	}

	public void showMessage(String title, String message, boolean error)
	{
		JOptionPane.showMessageDialog(this, message, title,
			error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
	}

	public boolean confirm(String title, String message)
	{
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}

	private void confirmDelete(TripView trip, Consumer<String> onDeleteTrip)
	{
		int choice = JOptionPane.showConfirmDialog(this,
			"Delete the trip from " + UiFormat.dateTime(trip.getStartedAt()) + "? This can't be undone.",
			"Delete trip", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice == JOptionPane.YES_OPTION)
		{
			onDeleteTrip.accept(trip.getId());
		}
	}

	private void promptGoal(PanelActions actions)
	{
		String input = JOptionPane.showInputDialog(this,
			"How many Maggot King kills is your goal? (0 removes it)", "Set kill goal", JOptionPane.QUESTION_MESSAGE);
		if (input == null)
		{
			return;
		}
		try
		{
			int target = Integer.parseInt(input.trim().replace(",", ""));
			if (target < 0 || target > 1_000_000)
			{
				throw new NumberFormatException();
			}
			actions.setGoal(target);
		}
		catch (NumberFormatException e)
		{
			showMessage("Set kill goal", "Enter a whole number of kills, like 100.", true);
		}
	}

	private void confirmResetGoal(PanelActions actions)
	{
		if (confirm("Reset kill goal", "Start counting the goal again from 0 kills now?"))
		{
			actions.resetGoal();
		}
	}

	private void confirmClear(Runnable onClearHistory)
	{
		int choice = JOptionPane.showConfirmDialog(this,
			"Delete all Maggot King trip history for this account? This can't be undone.",
			"Clear all history", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice == JOptionPane.YES_OPTION)
		{
			onClearHistory.run();
		}
	}

	/**
	 * Tab content that fills the scroll pane's width and scrolls vertically.
	 */
	private static class ScrollableContent extends JPanel implements Scrollable
	{
		ScrollableContent()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(16, visibleRect.height - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
