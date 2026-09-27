package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * Side panel with Current Trip, History and Lifetime tabs. All methods run on the Swing thread.
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
		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		currentTab = new CurrentTripPanel(itemManager);
		historyTab = new HistoryPanel(itemManager, trip -> confirmDelete(trip, actions::deleteTrip));
		lifetimeTab = new LifetimePanel(actions, () -> confirmClear(actions::clearHistory));

		JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);

		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		MaterialTab current = new MaterialTab("Current Trip", tabs, currentTab);
		tabs.addTab(current);
		tabs.addTab(new MaterialTab("History", tabs, historyTab));
		tabs.addTab(new MaterialTab("Lifetime", tabs, lifetimeTab));
		tabs.select(current);

		readOnlyWarning.setFont(FontManager.getRunescapeSmallFont());
		readOnlyWarning.setForeground(UiFormat.LOSS);
		readOnlyWarning.setText("<html>History could not be loaded or is from a newer version. Changes will not be saved.</html>");
		readOnlyWarning.setVisible(false);

		JPanel north = new JPanel(new BorderLayout(0, 4));
		north.setOpaque(false);
		north.add(tabs, BorderLayout.NORTH);
		north.add(readOnlyWarning, BorderLayout.SOUTH);

		add(north, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);

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
}
