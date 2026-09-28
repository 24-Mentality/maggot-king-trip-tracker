package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.view.DrynessView;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Expected vs received drops per unique, with an Expected tab (progress to the next statistical drop) and a
 * Received tab (luck: actual minus expected).
 */
class DropChancesCard extends JPanel
{
	private static final String HELP = "<html><b>Expected tab:</b><br>"
		+ "<u>Bar</u>: Progress toward the next statistical drop.<br>"
		+ "<u>Number</u>: Total drops you are expected to have.<br><br>"
		+ "<b>Received tab:</b><br>"
		+ "<u>Bar</u>: Your 'luck' (actual drops vs. expectations).<br>"
		+ "<u>Number</u>: Total drops you have actually received.<br><br>"
		+ "Uniques and the kill pet only come from Open-stomach; eggs you pop add to the pet row."
		+ " All-time numbers come from RuneLite's Loot Tracker when it has a record for this account.</html>";

	/**
	 * Remembered for the session, like the tab choice in other trackers.
	 */
	private static boolean showReceived;

	private final JLabel expectedTab = tabLabel("Expected");
	private final JLabel receivedTab = tabLabel("Received");
	private final List<Row> rows = new ArrayList<>();
	private final JLabel source = new JLabel();
	private DrynessView dryness;

	DropChancesCard(ItemManager itemManager)
	{
		setLayout(new BorderLayout(0, 3));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(3, 6, 6, 6));

		JLabel help = new JLabel(new HelpIcon());
		help.setToolTipText(HELP);

		expectedTab.addMouseListener(select(false));
		receivedTab.addMouseListener(select(true));
		JPanel tabs = new JPanel(new GridLayout(1, 2, 4, 0));
		tabs.setOpaque(false);
		tabs.add(expectedTab);
		tabs.add(receivedTab);

		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setOpaque(false);
		header.add(tabs, BorderLayout.CENTER);
		header.add(help, BorderLayout.EAST);

		JPanel rowPanel = new JPanel(new GridLayout(0, 1, 0, 3));
		rowPanel.setOpaque(false);
		rows.add(new Row(anyLabel()));
		rows.add(new Row(itemIcon(itemManager, MaggotKingIds.UNIQUES_FANG)));
		rows.add(new Row(itemIcon(itemManager, MaggotKingIds.UNIQUES_KISTEN)));
		rows.add(new Row(itemIcon(itemManager, MaggotKingIds.PET_ITEM)));
		for (Row row : rows)
		{
			rowPanel.add(row.panel);
		}

		source.setFont(FontManager.getRunescapeSmallFont());
		source.setForeground(UiFormat.MUTED_TEXT);

		add(header, BorderLayout.NORTH);
		add(rowPanel, BorderLayout.CENTER);
		add(source, BorderLayout.SOUTH);
		styleTabs();
	}

	void update(DrynessView dryness)
	{
		this.dryness = dryness;
		refresh();
	}

	private void refresh()
	{
		if (dryness == null)
		{
			return;
		}

		double eggPetExpected = 0;
		for (DrynessView.EggTier tier : dryness.getEggTiers())
		{
			eggPetExpected += tier.getPopped() * tier.getPetRate();
		}

		DrynessView.Unique fang = unique(MaggotKingIds.UNIQUES_FANG);
		DrynessView.Unique kisten = unique(MaggotKingIds.UNIQUES_KISTEN);
		DrynessView.AllTime allTime = dryness.getAllTime();
		double fangRate = MaggotKingRates.UNIQUES.get(MaggotKingIds.UNIQUES_FANG);
		double kistenRate = MaggotKingRates.UNIQUES.get(MaggotKingIds.UNIQUES_KISTEN);

		int kills;
		double[] expected;
		int[] received;
		String basis;
		if (allTime != null)
		{
			// All-time: the Loot Tracker records every loot-dropping (Open-stomach) kill and its drops
			kills = allTime.getLootKills();
			expected = new double[]{
				kills * MaggotKingRates.ANY_UNIQUE,
				kills * fangRate,
				kills * kistenRate,
				kills * MaggotKingRates.PET_PER_STOMACH + eggPetExpected,
			};
			received = new int[]{
				allTime.getFang() + allTime.getKisten(),
				allTime.getFang(),
				allTime.getKisten(),
				allTime.getPets(),
			};
			basis = String.format(Locale.ROOT, "%,d kills recorded by RuneLite's Loot Tracker since %s", kills,
				UiFormat.date(allTime.getFirstRecordedAt()))
				+ (allTime.getKillCount() != null ? String.format(Locale.ROOT, " (KC %,d)", allTime.getKillCount()) : "");
			source.setText(String.format(Locale.ROOT, "All-time · %,d kills", kills)
				+ (allTime.getKillCount() != null ? String.format(Locale.ROOT, " · KC %,d", allTime.getKillCount()) : ""));
		}
		else
		{
			kills = dryness.getStomachKills();
			expected = new double[]{
				dryness.getExpectedUniques(),
				fang == null ? 0 : fang.getExpected(),
				kisten == null ? 0 : kisten.getExpected(),
				dryness.getExpectedPetsFromKills() + eggPetExpected,
			};
			received = new int[]{
				dryness.getUniquesReceived(),
				fang == null ? 0 : fang.getKillCounts().size(),
				kisten == null ? 0 : kisten.getKillCounts().size(),
				dryness.getPetsFromKills() + dryness.getPetsFromEggs(),
			};
			basis = kills + " Open-stomach kills tracked by this plugin";
			source.setText("Tracked · " + kills + " kills (enable Loot Tracker for all-time)");
		}
		source.setToolTipText(UiFormat.tooltip("Based on " + basis + "."));
		String[] names = {"Any unique (1/205.6)", fangName(fang), kistenName(kisten), "Maggot King pet (1/3,500 per kill, plus eggs)"};

		double scale = 1;
		for (int i = 0; i < rows.size(); i++)
		{
			scale = Math.max(scale, Math.abs(received[i] - expected[i]));
		}

		for (int i = 0; i < rows.size(); i++)
		{
			Row row = rows.get(i);
			double whole = Math.floor(expected[i]);
			double toNext = expected[i] - whole;
			if (showReceived)
			{
				row.bar.showReceived(received[i] - expected[i], scale);
				row.number.setText(String.valueOf(received[i]));
			}
			else
			{
				row.bar.showExpected(toNext);
				row.number.setText(String.valueOf((int) whole));
			}
			String help = names[i] + ": " + received[i] + " received, "
				+ String.format(Locale.ROOT, "%.2f", expected[i]) + " expected from " + basis
				+ (i == 3 && eggPetExpected > 0 ? String.format(Locale.ROOT, " plus eggs popped (%.3f)", eggPetExpected) : "")
				+ ". " + String.format(Locale.ROOT, "%.0f%%", toNext * 100) + " of the way to the next expected drop."
				+ (i == 3 && allTime != null ? " The Loot Tracker doesn't record pets, so pets are the ones this plugin saw." : "");
			row.panel.setToolTipText(UiFormat.tooltip(help));
			row.bar.setToolTipText(UiFormat.tooltip(help));
		}
	}

	private DrynessView.Unique unique(int itemId)
	{
		for (DrynessView.Unique unique : dryness.getUniques())
		{
			if (unique.getItemId() == itemId)
			{
				return unique;
			}
		}
		return null;
	}

	private static String fangName(DrynessView.Unique fang)
	{
		return (fang == null ? "Elder venator fang" : fang.getName()) + " (1/"
			+ Math.round(1 / MaggotKingRates.UNIQUES.get(MaggotKingIds.UNIQUES_FANG)) + ")";
	}

	private static String kistenName(DrynessView.Unique kisten)
	{
		return (kisten == null ? "Crimson kisten" : kisten.getName()) + " (1/"
			+ Math.round(1 / MaggotKingRates.UNIQUES.get(MaggotKingIds.UNIQUES_KISTEN)) + ")";
	}

	private MouseAdapter select(boolean received)
	{
		return new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				showReceived = received;
				styleTabs();
				refresh();
			}
		};
	}

	private void styleTabs()
	{
		style(expectedTab, !showReceived);
		style(receivedTab, showReceived);
	}

	private static void style(JLabel tab, boolean selected)
	{
		tab.setForeground(selected ? Color.WHITE : UiFormat.MUTED_TEXT);
		tab.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, selected ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(2, 0, 2, 0)));
	}

	private static JLabel tabLabel(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeFont());
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return label;
	}

	private static JLabel anyLabel()
	{
		JLabel label = new JLabel("Any", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.YELLOW);
		return label;
	}

	private static JLabel itemIcon(ItemManager itemManager, int itemId)
	{
		JLabel label = new JLabel();
		label.setHorizontalAlignment(SwingConstants.CENTER);
		if (itemManager == null)
		{
			return label;
		}
		AsyncBufferedImage image = itemManager.getImage(itemId);
		Runnable scaled = () -> label.setIcon(new ImageIcon(ImageUtil.resizeImage(image, 23, 20)));
		image.onLoaded(scaled);
		scaled.run();
		return label;
	}

	private static class Row
	{
		final JPanel panel = new JPanel(new BorderLayout(4, 0));
		final DropBar bar = new DropBar();
		final JLabel number = new JLabel("0", SwingConstants.RIGHT);

		Row(JLabel icon)
		{
			panel.setOpaque(false);
			icon.setPreferredSize(new Dimension(26, 20));
			number.setFont(FontManager.getRunescapeSmallFont());
			number.setForeground(new Color(229, 229, 229));
			number.setPreferredSize(new Dimension(22, 20));
			panel.add(icon, BorderLayout.WEST);
			panel.add(bar, BorderLayout.CENTER);
			panel.add(number, BorderLayout.EAST);
		}
	}

	/**
	 * A circled question mark.
	 */
	private static class HelpIcon implements Icon
	{
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(UiFormat.MUTED_TEXT);
			g2.setStroke(new BasicStroke(1.2f));
			g2.drawOval(x + 1, y + 1, 13, 13);
			g2.setFont(FontManager.getRunescapeSmallFont());
			g2.drawString("?", x + 5, y + 12);
			g2.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return 16;
		}

		@Override
		public int getIconHeight()
		{
			return 16;
		}
	}
}
