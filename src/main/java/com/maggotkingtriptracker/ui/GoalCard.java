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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

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
	private final JButton pauseButton = smallButton("Pause");
	private boolean paused;

	private GoalView goal;

	GoalCard(ItemManager itemManager, Runnable onSetGoal, Runnable onPause, Runnable onReset)
	{
		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(24, 18));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		if (itemManager != null)
		{
			AsyncBufferedImage image = itemManager.getImage(MaggotKingIds.PET_ITEM);
			Runnable scaled = () -> icon.setIcon(new ImageIcon(ImageUtil.resizeImage(image, 21, 18)));
			image.onLoaded(scaled);
			scaled.run();
		}

		JPanel stats = new JPanel(new GridLayout(2, 2, 4, 0));
		stats.setOpaque(false);
		for (JLabel label : new JLabel[]{kph, done, ttg, left})
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			stats.add(label);
		}

		// The icon sits beside the bar so the four stats get the card's full width
		JPanel barRow = new JPanel(new BorderLayout(4, 0));
		barRow.setOpaque(false);
		barRow.add(icon, BorderLayout.WEST);
		barRow.add(progress, BorderLayout.CENTER);

		progress.setBackground(BAR_BACKGROUND);
		progress.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progress.setPreferredSize(new Dimension(0, 16));
		progress.setLeftLabel("");
		progress.setRightLabel("");

		JButton setButton = smallButton("Set goal");
		setButton.addActionListener(e -> onSetGoal.run());
		pauseButton.addActionListener(e -> onPause.run());
		pauseButton.setToolTipText(UiFormat.tooltip("Stop the trip clock and the goal clock while you're AFK. Kills,"
			+ " loot and supplies still count. Resumes when you press it again or attack the boss."));
		resetButton.addActionListener(e -> onReset.run());
		// Equal widths: a grid, not a row of natural-width buttons
		JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
		buttons.setOpaque(false);
		buttons.add(setButton);
		buttons.add(pauseButton);
		buttons.add(resetButton);

		add(stats, BorderLayout.NORTH);
		add(barRow, BorderLayout.CENTER);
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

	void setPaused(boolean paused)
	{
		this.paused = paused;
		pauseButton.setText(paused ? "Resume" : "Pause");
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
			String.format(Locale.ROOT, "%,d", doneKills),
			remaining == 0 ? "Done" : killsPerHour > 0 ? timeToGoal((long) (remaining / killsPerHour * 3_600_000)) : NOT_AVAILABLE,
			String.format(Locale.ROOT, "%,d", remaining));

		progress.setMaximumValue(Math.max(1, target));
		progress.setValue(Math.min(doneKills, target));
		double percent = Math.min(100, doneKills * 100.0 / Math.max(1, target));
		progress.setCenterLabel(String.format(Locale.ROOT, "%.1f%%", percent));
		setToolTipText("Goal: " + target + " kills · logged-in time " + UiFormat.duration(activeMs)
			+ (paused ? " · paused" : ""));
	}

	private static String timeToGoal(long ms)
	{
		return ms >= 100L * 3_600_000 ? "100h+" : UiFormat.duration(ms);
	}

	private void setStats(String kphValue, String doneValue, String ttgValue, String leftValue)
	{
		// Time-based values are greyed while paused
		Color clock = paused ? UiFormat.MUTED_TEXT : null;
		kph.setText(UiFormat.pair("KPH", kphValue, clock));
		done.setText(UiFormat.pair("Kills Done", doneValue));
		ttg.setText(UiFormat.pair("TTG", ttgValue, clock));
		left.setText(UiFormat.pair("Kills Left", leftValue));
	}
}
