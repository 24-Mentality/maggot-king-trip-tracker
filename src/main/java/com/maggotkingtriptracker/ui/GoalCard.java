package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.view.GoalView;
import java.awt.BorderLayout;
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
 * Kill goal progress: kills per hour, kills done and left, time to goal, and a progress bar.
 */
class GoalCard extends JPanel
{
	private final JLabel title = new JLabel();
	private final StatCell kph = new StatCell("KPH", false);
	private final StatCell done = new StatCell("Done", false);
	private final StatCell left = new StatCell("Left", false);
	private final StatCell ttg = new StatCell("TTG", false);
	private final JPanel stats = new JPanel(new GridLayout(1, 4, 4, 0));
	private final ProgressBar progress = new ProgressBar();
	private final JButton resetButton = smallButton("Reset");

	private GoalView goal;

	GoalCard(ItemManager itemManager, Runnable onSetGoal, Runnable onReset)
	{
		setLayout(new BorderLayout(0, 3));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 6, 5, 6));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(24, 22));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		AsyncBufferedImage image = itemManager.getImage(MaggotKingIds.PET_ITEM);
		Runnable setIcon = () -> icon.setIcon(new ImageIcon(ImageUtil.resizeImage(image, 24, 21)));
		image.onLoaded(setIcon);
		setIcon.run();

		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JButton setButton = smallButton("Set");
		setButton.addActionListener(e -> onSetGoal.run());
		resetButton.addActionListener(e -> onReset.run());
		JPanel buttons = new JPanel(new GridLayout(1, 2, 3, 0));
		buttons.setOpaque(false);
		buttons.add(setButton);
		buttons.add(resetButton);

		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setOpaque(false);
		header.add(icon, BorderLayout.WEST);
		header.add(title, BorderLayout.CENTER);
		header.add(buttons, BorderLayout.EAST);

		stats.setOpaque(false);
		stats.add(kph);
		stats.add(done);
		stats.add(left);
		stats.add(ttg);

		progress.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progress.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progress.setPreferredSize(new Dimension(0, 14));

		add(header, BorderLayout.NORTH);
		add(stats, BorderLayout.CENTER);
		add(progress, BorderLayout.SOUTH);
	}

	private static JButton smallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setMargin(new Insets(1, 4, 1, 4));
		button.setFocusPainted(false);
		return button;
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
			title.setText("No kill goal");
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

		title.setText("Goal " + doneKills + " / " + target);
		title.setToolTipText("Logged-in time since the goal was set: " + UiFormat.duration(activeMs));
		kph.setValue(killsPerHour > 0 ? String.format(Locale.ROOT, "%.1f", killsPerHour) : "-");
		done.setValue(String.valueOf(doneKills));
		left.setValue(String.valueOf(remaining));
		ttg.setValue(remaining == 0 ? "Done" : killsPerHour > 0
			? UiFormat.duration((long) (remaining / killsPerHour * 3_600_000)) : "-");

		progress.setMaximumValue(Math.max(1, target));
		progress.setValue(Math.min(doneKills, target));
		int percent = (int) Math.min(100, Math.floor(doneKills * 100.0 / Math.max(1, target)));
		progress.setCenterLabel(percent + "%");
		progress.setLeftLabel("");
		progress.setRightLabel("");
	}
}
