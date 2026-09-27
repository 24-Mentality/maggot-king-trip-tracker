package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.model.DropOdds;
import com.maggotkingtriptracker.view.DrynessView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ProgressBar;

/**
 * Luck on uniques (Elder venator fang, Crimson kisten) from the kills this plugin has tracked: received vs
 * expected, how dry you are, and when the next unique is due at the drop rate.
 */
class LuckCard extends JPanel
{
	private static final Color HEADER_BORDER = new Color(57, 57, 57);
	private static final Color BAR_BACKGROUND = new Color(61, 56, 49);
	private static final double RATE = MaggotKingRates.ANY_UNIQUE;

	private final JLabel uniques = statLabel();
	private final JLabel luck = statLabel();
	private final JLabel dry = statLabel();
	private final JLabel byNow = statLabel();
	private final JLabel next = statLabel();
	private final JLabel odds = statLabel();
	private final ProgressBar progress = new ProgressBar();
	private final JPanel body = new JPanel(new BorderLayout(0, 3));
	private final JLabel eye = new JLabel();
	private boolean hidden;

	LuckCard()
	{
		setLayout(new BorderLayout(0, 1));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(HEADER_BORDER, 1),
			BorderFactory.createEmptyBorder(3, 6, 5, 6)));

		JLabel title = new JLabel("Luck");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		title.setToolTipText(UiFormat.tooltip("Based on the Open-stomach kills this plugin has tracked (uniques only"
			+ " come from Open-stomach). The in-game collection log can't be read unless it's open, so drops from"
			+ " before you installed the plugin aren't included."));

		eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		eye.setHorizontalAlignment(SwingConstants.RIGHT);
		eye.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				hidden = !hidden;
				applyVisibility();
			}
		});

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);
		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(eye, BorderLayout.EAST);

		JPanel stats = new JPanel(new GridLayout(3, 2, 6, 0));
		stats.setOpaque(false);
		for (JLabel label : new JLabel[]{uniques, luck, dry, byNow, next, odds})
		{
			stats.add(label);
		}

		progress.setBackground(BAR_BACKGROUND);
		progress.setForeground(ColorScheme.BRAND_ORANGE);
		progress.setPreferredSize(new Dimension(0, 14));
		progress.setLeftLabel("");
		progress.setRightLabel("");

		body.setOpaque(false);
		body.add(stats, BorderLayout.NORTH);
		body.add(progress, BorderLayout.CENTER);

		add(titleRow, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		applyVisibility();
	}

	private static JLabel statLabel()
	{
		JLabel label = new JLabel();
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	private void applyVisibility()
	{
		body.setVisible(!hidden);
		eye.setIcon(new EyeIcon(hidden));
		eye.setToolTipText(hidden ? "Show luck" : "Hide luck");
		revalidate();
		repaint();
	}

	void update(DrynessView dryness)
	{
		int received = dryness.getUniquesReceived();
		double expected = dryness.getExpectedUniques();
		int since = dryness.getStomachKillsSinceUnique();
		Integer currentKc = dryness.getCurrentKc();
		Integer lastKc = dryness.getLastUniqueKc();

		StringBuilder breakdown = new StringBuilder();
		for (DrynessView.Unique unique : dryness.getUniques())
		{
			breakdown.append('\n').append(unique.getName()).append(": ").append(unique.getKillCounts().size())
				.append(" (").append(String.format(Locale.ROOT, "%.2f", unique.getExpected())).append(" expected, 1/")
				.append(Math.round(1 / MaggotKingRates.UNIQUES.get(unique.getItemId()))).append(')');
		}
		breakdown.append("\nPet: ").append(dryness.getPetsFromKills()).append(" (")
			.append(String.format(Locale.ROOT, "%.3f", dryness.getExpectedPetsFromKills())).append(" expected, 1/3,500)");
		set(uniques, "Uniques", received + " / " + String.format(Locale.ROOT, "%.2f", expected), null,
			"Uniques received vs expected from " + dryness.getStomachKills() + " tracked Open-stomach kills at 1/205.6."
				+ breakdown);

		double percentile = DropOdds.luckPercentile(received, expected);
		String verdict = dryness.getStomachKills() == 0 ? "N/A"
			: percentile >= 0.6 ? "Lucky" : percentile <= 0.4 ? "Dry" : "On rate";
		Color verdictColor = dryness.getStomachKills() == 0 ? null
			: percentile >= 0.6 ? UiFormat.PROFIT : percentile <= 0.4 ? UiFormat.LOSS : null;
		set(luck, "Luck", verdict, verdictColor, "Compared with other players after the same number of kills, you've had"
			+ " more uniques than about " + Math.round(percentile * 100) + "% of them (50% is exactly average)."
			+ " Lucky above 60%, dry below 40%.");

		String sinceWhat = lastKc != null ? "your last unique at KC " + String.format(Locale.ROOT, "%,d", lastKc)
			: dryness.getFirstTrackedKc() != null ? "tracking began at KC " + String.format(Locale.ROOT, "%,d", dryness.getFirstTrackedKc())
			: "tracking began";
		set(dry, "Dry", since + " kc", null, "Open-stomach kills since " + sinceWhat + ".");

		double chance = DropOdds.chanceByNow(RATE, since);
		set(byNow, "By now", percent(chance), null, "Chance of at least one unique in " + since
			+ " kills at 1/205.6. This is how many players would have had one by now.");

		int onRate = (int) Math.ceil(1 / RATE);
		int toGo = onRate - since;
		int half = DropOdds.killsForChance(RATE, 0.5);
		int ninety = DropOdds.killsForChance(RATE, 0.9);
		if (toGo > 0)
		{
			set(next, "Next", currentKc != null ? "KC " + String.format(Locale.ROOT, "%,d", currentKc + toGo) : toGo + " kc", null,
				"On average one unique drops every 205.6 kills, so the next is due in " + toGo + " more kills"
					+ (currentKc != null ? " (KC " + String.format(Locale.ROOT, "%,d", currentKc + toGo) + ")" : "") + ".");
		}
		else
		{
			set(next, "Overdue", "+" + (-toGo) + " kc", UiFormat.LOSS, "You're " + (-toGo)
				+ " kills past the 205.6-kill average. Each kill is still 1/205.6: drops don't become more likely.");
		}
		set(odds, "50% / 90%", half + " / " + ninety, null, "Kills after a unique by which 50% and 90% of players"
			+ " get the next one: " + half + " and " + ninety + ".");

		progress.setMaximumValue(onRate);
		progress.setValue(Math.min(since, onRate));
		progress.setCenterLabel(toGo > 0
			? String.format(Locale.ROOT, "%.0f%% of drop rate", since * 100.0 / onRate)
			: "Past drop rate");
		progress.setToolTipText(UiFormat.tooltip("Kills since your last unique (" + since + ") out of the 205.6-kill"
			+ " average between uniques."));
	}

	private static void set(JLabel label, String name, String value, Color color, String help)
	{
		label.setText(UiFormat.pair(name, value, color));
		label.setToolTipText(UiFormat.tooltip(help));
	}

	private static String percent(double chance)
	{
		double pct = chance * 100;
		return String.format(Locale.ROOT, pct < 1 ? "%.2f%%" : "%.1f%%", pct);
	}
}
