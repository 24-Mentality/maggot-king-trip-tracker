package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.view.GoalView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ProgressBar;

/**
 * Kill goal progress in the style of RuneLite's XP tracker: the Maggot King icon, KPH / Kills Done / TTG /
 * Kills Left, and a progress bar with the percentage.
 */
class GoalCard extends JPanel
{
	private static final Color BAR_BACKGROUND = new Color(61, 56, 49);
	private static final String NOT_AVAILABLE = "N/A";

	private final JLabel kph = new JLabel();
	private final JLabel done = new JLabel();
	private final JLabel ttg = new JLabel();
	private final JLabel left = new JLabel();
	private final ProgressBar progress = new ProgressBar();
	private final JButton resetButton = smallButton("Reset");

	private GoalView goal;

	GoalCard(ItemManager itemManager, Runnable onSetGoal, Runnable onReset)
	{
		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(38, 32));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		itemManager.getImage(MaggotKingIds.PET_ITEM).addTo(icon);

		JPanel stats = new JPanel(new GridLayout(2, 2, 4, 0));
		stats.setOpaque(false);
		for (JLabel label : new JLabel[]{kph, done, ttg, left})
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			stats.add(label);
		}

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(icon, BorderLayout.WEST);
		top.add(stats, BorderLayout.CENTER);

		progress.setBackground(BAR_BACKGROUND);
		progress.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progress.setPreferredSize(new Dimension(0, 16));
		progress.setLeftLabel("");
		progress.setRightLabel("");

		JButton setButton = smallButton("Set goal");
		setButton.addActionListener(e -> onSetGoal.run());
		resetButton.addActionListener(e -> onReset.run());
		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		buttons.add(setButton);
		buttons.add(resetButton);

		add(top, BorderLayout.NORTH);
		add(progress, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
	}

	private static JButton smallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setMargin(new Insets(0, 4, 0, 4));
		button.setFocusPainted(false);
		return button;
	}

	void setGoal(GoalView goal, long now)
	{
		this.goal = goal;
		resetButton.setEnabled(goal != null);
		if (goal == null)
		{
			setStats(NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
			progress.setMaximumValue(1);
			progress.setValue(0);
			progress.setCenterLabel("No goal set");
			setToolTipText(null);
			return;
		}
		tick(now);
	}

	void tick(long now)
	{
		if (goal == null)
		{
			return;
		}

		int doneKills = goal.getDone();
		int target = goal.getTarget();
		int remaining = Math.max(0, target - doneKills);
		long activeMs = goal.activeMsAt(now);
		double killsPerHour = activeMs >= 60_000 && doneKills > 0 ? doneKills * 3_600_000.0 / activeMs : 0;

		setStats(
			killsPerHour > 0 ? String.format(Locale.ROOT, "%.1f", killsPerHour) : NOT_AVAILABLE,
			String.valueOf(doneKills),
			remaining == 0 ? "Done" : killsPerHour > 0 ? UiFormat.duration((long) (remaining / killsPerHour * 3_600_000)) : NOT_AVAILABLE,
			String.valueOf(remaining));

		progress.setMaximumValue(Math.max(1, target));
		progress.setValue(Math.min(doneKills, target));
		double percent = Math.min(100, doneKills * 100.0 / Math.max(1, target));
		progress.setCenterLabel(String.format(Locale.ROOT, "%.1f%%", percent));
		setToolTipText("Goal: " + target + " kills · logged-in time " + UiFormat.duration(activeMs));
	}

	private void setStats(String kphValue, String doneValue, String ttgValue, String leftValue)
	{
		kph.setText(UiFormat.pair("KPH", kphValue));
		done.setText(UiFormat.pair("Kills Done", doneValue));
		ttg.setText(UiFormat.pair("TTG", ttgValue));
		left.setText(UiFormat.pair("Kills Left", leftValue));
	}
}
