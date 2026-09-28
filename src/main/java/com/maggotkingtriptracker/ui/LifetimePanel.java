package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.DrynessView;
import com.maggotkingtriptracker.view.ItemView;
import com.maggotkingtriptracker.view.LifetimeView;
import com.maggotkingtriptracker.view.PolishView;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class LifetimePanel extends JPanel
{
	private final StatCell trips = new StatCell("Trips", false);
	private final StatCell kills = new StatCell("Kills", false);
	private final StatCell time = new StatCell("Lair time", false);
	private final StatCell averageKill = new StatCell("Avg kill", false);
	private final StatCell loot = new StatCell("Loot", false);
	private final StatCell costs = new StatCell("Costs", false);
	private final StatCell net = new StatCell("Net", true);
	private final StatCell gpPerHour = new StatCell("GP/hr", true);
	private final StatCell deaths = new StatCell("Deaths", false);
	private final JLabel details = new JLabel();
	private final JLabel todayValue = new JLabel();
	private final JLabel chartTitle = new JLabel();
	private final ProfitTrendChart chart = new ProfitTrendChart();
	private final InfoCard drynessCard = new InfoCard("Dryness");
	private final InfoCard eggCard = new InfoCard("Eggs popped");
	private final InfoCard polishCard = new InfoCard("Polish results");
	private final JButton exportCsvButton = new JButton("Export CSV");
	private final JButton exportJsonButton = new JButton("Export JSON");
	private final JButton importJsonButton = new JButton("Import JSON");
	private final JButton clearButton = new JButton("Clear all");

	LifetimePanel(PanelActions actions, Runnable onClear)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel card = new JPanel(new BorderLayout(0, 3));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		card.setAlignmentX(LEFT_ALIGNMENT);

		JPanel grid = new JPanel(new GridLayout(3, 3, 4, 3));
		grid.setOpaque(false);
		for (StatCell cell : new StatCell[]{trips, kills, time, loot, costs, net, gpPerHour, averageKill, deaths})
		{
			grid.add(cell);
		}

		JPanel footer = new JPanel(new GridLayout(0, 1, 0, 0));
		footer.setOpaque(false);
		for (JLabel label : new JLabel[]{details, todayValue})
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(UiFormat.MUTED_TEXT);
			footer.add(label);
		}

		card.add(grid, BorderLayout.CENTER);
		card.add(footer, BorderLayout.SOUTH);
		add(card);

		chartTitle.setFont(FontManager.getRunescapeBoldFont());
		chartTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		chartTitle.setFont(FontManager.getRunescapeSmallFont());
		chartTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
		chartTitle.setAlignmentX(LEFT_ALIGNMENT);
		add(chartTitle);
		chart.setAlignmentX(LEFT_ALIGNMENT);
		add(chart);

		for (InfoCard infoCard : new InfoCard[]{drynessCard, eggCard, polishCard})
		{
			add(spacer());
			add(infoCard);
		}

		JLabel dataTitle = new JLabel("Data");
		dataTitle.setFont(FontManager.getRunescapeBoldFont());
		dataTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dataTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
		dataTitle.setAlignmentX(LEFT_ALIGNMENT);
		add(dataTitle);

		JPanel buttons = new JPanel(new GridLayout(0, 2, 4, 4));
		buttons.setOpaque(false);
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		exportCsvButton.addActionListener(e -> actions.exportCsv());
		exportJsonButton.addActionListener(e -> actions.exportJson());
		importJsonButton.addActionListener(e -> actions.importJson());
		clearButton.setForeground(UiFormat.LOSS);
		clearButton.addActionListener(e -> onClear.run());
		for (JButton button : new JButton[]{exportCsvButton, exportJsonButton, importJsonButton, clearButton})
		{
			button.setFocusPainted(false);
			button.setFont(FontManager.getRunescapeSmallFont());
			button.setMargin(new Insets(2, 2, 2, 2));
			buttons.add(button);
		}
		add(buttons);
	}

	private static JPanel spacer()
	{
		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setAlignmentX(LEFT_ALIGNMENT);
		spacer.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return spacer;
	}

	void update(LifetimeView view, boolean readOnly)
	{
		if (view == null)
		{
			setVisible(false);
			return;
		}
		setVisible(true);

		trips.setValue(String.valueOf(view.getTrips()));
		kills.setValue(String.valueOf(view.getKills()));
		time.setValue(UiFormat.duration(view.getActiveMs()));
		averageKill.setValue(UiFormat.killTime(view.getAverageKillMs()));
		loot.setValue(UiFormat.gp(view.getLootValue()), ColorScheme.LIGHT_GRAY_COLOR,
			UiFormat.fullGp(view.getLootValue()) + " at recorded prices");

		long totalCosts = view.getSupplyCost() + view.getDroppedCost() + view.getDeathCost();
		costs.setValue(UiFormat.gp(totalCosts), ColorScheme.LIGHT_GRAY_COLOR, "<html>Supplies: " + UiFormat.fullGp(view.getSupplyCost())
			+ "<br>Dropped: " + UiFormat.fullGp(view.getDroppedCost())
			+ "<br>Deaths: " + UiFormat.fullGp(view.getDeathCost()) + "</html>");
		net.setValue(UiFormat.gp(view.getNetProfit()), UiFormat.profitColor(view.getNetProfit()), UiFormat.fullGp(view.getNetProfit()));
		long rate = TripMath.gpPerHour(view.getNetProfit(), view.getActiveMs());
		gpPerHour.setValue(UiFormat.gp(rate), UiFormat.profitColor(rate), UiFormat.fullGp(rate) + " per hour in the lair");
		deaths.setValue(String.valueOf(view.getDeaths()));

		details.setText("Stomach " + view.getStomachKills() + " · Eggs " + view.getEggKills()
			+ (view.getPets() > 0 ? " · Pets " + view.getPets() : ""));

		Long today = view.getLootValueToday();
		todayValue.setVisible(today != null);
		if (today != null)
		{
			todayValue.setText("Loot at today's prices: " + UiFormat.gp(today));
			todayValue.setToolTipText(UiFormat.fullGp(today));
		}

		int shown = Math.min(view.getNetPerTrip().size(), ProfitTrendChart.MAX_TRIPS);
		chartTitle.setText(shown == 0 ? "Profit per trip" : "Profit per trip (last " + shown + ")");
		chart.setValues(view.getNetPerTrip());

		updateDryness(view.getDryness());
		updatePolish(view.getPolish());

		exportCsvButton.setEnabled(view.getTrips() > 0);
		exportJsonButton.setEnabled(view.getTrips() > 0);
		importJsonButton.setEnabled(!readOnly);
		clearButton.setEnabled(!readOnly && view.getTrips() > 0);
		revalidate();
		repaint();
	}

	private void updateDryness(DrynessView dryness)
	{
		// One short fact per line so nothing wraps at sidebar width; details are in the hover text
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[]{"Stomach kills: " + String.format(Locale.ROOT, "%,d", dryness.getStomachKills()),
			"Open-stomach kills tracked by this plugin. Uniques and the kill pet only come from Open-stomach."});
		rows.add(new String[]{"Since unique: " + String.format(Locale.ROOT, "%,d", dryness.getStomachKillsSinceUnique())
			+ " (" + percent(dryness.getChanceThisDry()) + " this dry)",
			"Open-stomach kills since your last tracked unique, and the chance of going that long without one at 1/205.6."});
		for (DrynessView.Unique unique : dryness.getUniques())
		{
			List<String> kcs = new ArrayList<>();
			for (Integer kc : unique.getKillCounts())
			{
				kcs.add(kc == null ? "?" : String.format(Locale.ROOT, "%,d", kc));
			}
			rows.add(new String[]{unique.getName() + ": " + unique.getKillCounts().size() + " / "
				+ String.format(Locale.ROOT, "%.2f", unique.getExpected()),
				"Received / expected from tracked kills (1/" + Math.round(1 / MaggotKingRates.UNIQUES.get(unique.getItemId()))
					+ ")." + (kcs.isEmpty() ? "" : " Received at KC " + String.join(", ", kcs) + ".")});
		}
		rows.add(new String[]{"Pet from kills: " + dryness.getPetsFromKills() + " / "
			+ String.format(Locale.ROOT, "%.3f", dryness.getExpectedPetsFromKills()),
			"Received / expected from tracked Open-stomach kills at 1/3,500."});
		drynessCard.setRows(rows);

		List<String[]> eggRows = new ArrayList<>();
		int popped = 0;
		for (DrynessView.EggTier tier : dryness.getEggTiers())
		{
			// Only tiers you've popped, to keep the card short
			if (tier.getPopped() == 0)
			{
				continue;
			}
			popped += tier.getPopped();
			eggRows.add(new String[]{tier.getName() + ": " + String.format(Locale.ROOT, "%,d", tier.getPopped())
				+ (tier.getPets() > 0 ? " · pets " + tier.getPets() : ""),
				"Popped, with a 1/" + Math.round(1 / tier.getPetRate()) + " pet chance each."});
		}
		eggRows.add(popped == 0 ? new String[]{"No eggs popped yet", null}
			: new String[]{"Egg pet chance: " + percent(dryness.getEggPetChance())
			+ (dryness.getPetsFromEggs() > 0 ? " · pets " + dryness.getPetsFromEggs() : ""),
			"Chance of at least one pet from all the eggs popped so far."});
		eggCard.setRows(eggRows);
	}

	private void updatePolish(List<PolishView> polish)
	{
		// The type on one line, then one line per result
		List<String> rows = new ArrayList<>();
		for (PolishView view : polish)
		{
			rows.add(view.getTarnishedName() + " (" + view.getTotal() + ")");
			for (ItemView outcome : view.getOutcomes())
			{
				rows.add("  " + outcome.getName() + " x" + outcome.getQuantity());
			}
		}
		if (rows.isEmpty())
		{
			rows.add("Nothing polished yet");
		}
		polishCard.setLines(rows);
	}

	private static String percent(double chance)
	{
		double pct = chance * 100;
		return String.format(Locale.ROOT, pct < 1 ? "%.2f%%" : "%.1f%%", pct);
	}
}
