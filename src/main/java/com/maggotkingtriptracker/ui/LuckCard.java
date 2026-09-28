package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.model.DropOdds;
import com.maggotkingtriptracker.model.LuckTier;
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
	private final JLabel tier = new JLabel();
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

		JLabel title = new JLabel("Luck Status:");
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

		// The tier sits beside the title so it stays visible when the card is collapsed
		tier.setFont(FontManager.getRunescapeBoldFont());
		JPanel titleLeft = new JPanel(new BorderLayout(6, 0));
		titleLeft.setOpaque(false);
		titleLeft.add(title, BorderLayout.WEST);
		titleLeft.add(tier, BorderLayout.CENTER);

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);
		titleRow.add(titleLeft, BorderLayout.CENTER);
		titleRow.add(eye, BorderLayout.EAST);

		JPanel stats = new JPanel(new GridLayout(2, 2, 4, 0));
		stats.setOpaque(false);
		for (JLabel label : new JLabel[]{uniques, dry, byNow, next})
		{
			stats.add(label);
		}

		JPanel statRows = new JPanel(new BorderLayout());
		statRows.setOpaque(false);
		statRows.add(stats, BorderLayout.NORTH);
		statRows.add(odds, BorderLayout.CENTER);

		progress.setBackground(BAR_BACKGROUND);
		progress.setForeground(ColorScheme.BRAND_ORANGE);
		progress.setPreferredSize(new Dimension(0, 14));
		progress.setLeftLabel("");
		progress.setRightLabel("");

		body.setOpaque(false);
		body.add(statRows, BorderLayout.NORTH);
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
		DrynessView.AllTime allTime = dryness.getAllTime();
		int received = allTime != null ? allTime.getFang() + allTime.getKisten() : dryness.getUniquesReceived();
		double expected = allTime != null ? allTime.getLootKills() * RATE : dryness.getExpectedUniques();
		int basisKills = allTime != null ? allTime.getLootKills() : dryness.getStomachKills();
		int since = dryness.getStomachKillsSinceUnique();
		Integer currentKc = dryness.getCurrentKc();
		Integer lastKc = dryness.getLastUniqueKc();

		StringBuilder breakdown = new StringBuilder();
		for (DrynessView.Unique unique : dryness.getUniques())
		{
			double rate = MaggotKingRates.UNIQUES.get(unique.getItemId());
			int got = allTime == null ? unique.getKillCounts().size()
				: unique.getItemId() == MaggotKingIds.UNIQUES_FANG ? allTime.getFang() : allTime.getKisten();
			breakdown.append('\n').append(unique.getName()).append(": ").append(got)
				.append(" (").append(String.format(Locale.ROOT, "%.2f", basisKills * rate)).append(" expected, 1/")
				.append(Math.round(1 / rate)).append(')');
		}
		String basis = allTime != null
			? String.format(Locale.ROOT, "%,d kills recorded by RuneLite's Loot Tracker (all-time)", basisKills)
			: basisKills + " Open-stomach kills tracked by this plugin";
		// One decimal once expected reaches 10, so large counts still fit half the card
		set(uniques, "Uniques", received + " / " + String.format(Locale.ROOT, expected >= 10 ? "%.1f" : "%.2f", expected), null,
			"Uniques received vs expected from " + basis + " at 1/205.6." + breakdown);

		double percentile = DropOdds.luckPercentile(received, expected);
		if (basisKills == 0)
		{
			// No tier without kills
			tier.setText("");
			tier.setToolTipText(null);
		}
		else
		{
			LuckTier luckTier = LuckTier.of(percentile);
			tier.setText(luckTier.getLabel());
			tier.setForeground(tierColor(luckTier));
			tier.setToolTipText(UiFormat.tooltip("You've had more uniques than about " + Math.round(percentile * 100)
				+ "% of players with the same kills (50% is exactly average).\n\n"
				+ "LUCKY AS RUCK: 90% and up\nLucky: 65% to 90%\nOn Rate: 35% to 65%\nDry: 10% to 35%\n"
				+ "DRY AS RUCK: 10% and down"));
		}

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
		set(odds, "50% / 90% of players by", half + " / " + ninety + " kc", null, "Kills after a unique by which 50%"
			+ " and 90% of players get the next one: " + half + " and " + ninety + ".");

		progress.setMaximumValue(onRate);
		progress.setValue(Math.min(since, onRate));
		// The bar's centre label only gets a third of its width, so keep it short
		progress.setCenterLabel(toGo > 0 ? String.format(Locale.ROOT, "%.0f%%", since * 100.0 / onRate) : "Past rate");
		progress.setToolTipText(UiFormat.tooltip("Progress to the drop rate: kills since your last unique (" + since
			+ ") out of the 205.6-kill average between uniques."));
	}

	private static Color tierColor(LuckTier tier)
	{
		switch (tier)
		{
			case LUCKY_AS_RUCK:
				return UiFormat.UNIQUE_BORDER;
			case LUCKY:
				return UiFormat.PROFIT;
			case DRY:
				return ColorScheme.BRAND_ORANGE;
			case DRY_AS_RUCK:
				return UiFormat.LOSS;
			default:
				return ColorScheme.LIGHT_GRAY_COLOR;
		}
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
