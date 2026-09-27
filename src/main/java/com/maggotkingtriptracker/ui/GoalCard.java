package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.view.GoalView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
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
 * Kill goal progress: kills per hour, kills done and left, time to goal, and a progress bar.
 */
class GoalCard extends JPanel
{
	private final JLabel title = new JLabel();
	private final JLabel subtitle = new JLabel();
	private final StatCell kph = new StatCell("KPH", false);
	private final StatCell done = new StatCell("Kills done", false);
	private final StatCell left = new StatCell("Kills left", false);
	private final StatCell ttg = new StatCell("TTG", false);
	private final JPanel stats = new JPanel(new GridLayout(2, 2, 6, 6));
	private final ProgressBar progress = new ProgressBar();
	private final JButton resetButton = new JButton("Reset");

	private GoalView goal;

	GoalCard(ItemManager itemManager, Runnable onSetGoal, Runnable onReset)
	{
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		itemManager.getImage(MaggotKingIds.PET_ITEM).addTo(icon);

		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(UiFormat.MUTED_TEXT);

		JPanel titles = new JPanel(new GridLayout(2, 1));
		titles.setOpaque(false);
		titles.add(title);
		titles.add(subtitle);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.add(icon, BorderLayout.WEST);
		header.add(titles, BorderLayout.CENTER);

		stats.setOpaque(false);
		stats.add(kph);
		stats.add(done);
		stats.add(left);
		stats.add(ttg);

		progress.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progress.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progress.setPreferredSize(new Dimension(0, 16));

		JButton setButton = new JButton("Set goal");
		setButton.setFocusPainted(false);
		setButton.addActionListener(e -> onSetGoal.run());
		resetButton.setFocusPainted(false);
		resetButton.addActionListener(e -> onReset.run());
		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(setButton);
		buttons.add(resetButton);

		JPanel body = new JPanel(new BorderLayout(0, 6));
		body.setOpaque(false);
		body.add(stats, BorderLayout.NORTH);
		body.add(progress, BorderLayout.CENTER);

		add(header, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
	}

	void setGoal(GoalView goal, long now)
	{
		this.goal = goal;
		boolean active = goal != null;
		stats.setVisible(active);
		progress.setVisible(active);
		resetButton.setEnabled(active);
		if (!active)
		{
			title.setText("Kill goal");
			subtitle.setText("No goal set");
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
		double killsPerHour = activeMs >= 60_000 ? doneKills * 3_600_000.0 / activeMs : 0;

		title.setText("Kill goal: " + doneKills + " / " + target);
		subtitle.setText(remaining == 0 ? "Goal reached!" : "Logged-in time " + UiFormat.duration(activeMs));
		kph.setValue(killsPerHour > 0 ? String.format(Locale.ROOT, "%.1f", killsPerHour) : "-");
		done.setValue(String.valueOf(doneKills));
		left.setValue(String.valueOf(remaining));
		ttg.setValue(remaining == 0 ? "Done" : killsPerHour > 0
			? UiFormat.duration((long) (remaining / killsPerHour * 3_600_000)) : "-");

		progress.setMaximumValue(Math.max(1, target));
		progress.setValue(Math.min(doneKills, target));
		int percent = (int) Math.min(100, Math.floor(doneKills * 100.0 / Math.max(1, target)));
		progress.setCenterLabel(percent + "%");
		progress.setLeftLabel("0");
		progress.setRightLabel(String.valueOf(target));
	}
}
