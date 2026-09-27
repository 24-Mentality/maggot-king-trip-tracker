package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.LifetimeView;
import java.awt.BorderLayout;
import java.awt.GridLayout;
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
	private final StatCell time = new StatCell("Time in lair", false);
	private final StatCell averageKill = new StatCell("Avg kill", false);
	private final StatCell loot = new StatCell("Loot", false);
	private final StatCell costs = new StatCell("Costs", false);
	private final StatCell net = new StatCell("Net profit", true);
	private final StatCell gpPerHour = new StatCell("GP/hr", false);
	private final StatCell deaths = new StatCell("Deaths", false);
	private final JLabel details = new JLabel();
	private final JLabel todayValue = new JLabel();
	private final JLabel chartTitle = new JLabel();
	private final ProfitTrendChart chart = new ProfitTrendChart();
	private final JButton clearButton = new JButton("Clear all history");

	LifetimePanel(Runnable onClear)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel card = new JPanel(new BorderLayout(0, 6));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		card.setAlignmentX(LEFT_ALIGNMENT);

		JPanel top = new JPanel(new GridLayout(3, 2, 6, 6));
		top.setOpaque(false);
		top.add(trips);
		top.add(kills);
		top.add(time);
		top.add(averageKill);
		top.add(loot);
		top.add(costs);

		JPanel bottom = new JPanel(new GridLayout(1, 2, 6, 6));
		bottom.setOpaque(false);
		bottom.add(gpPerHour);
		bottom.add(deaths);

		JPanel middle = new JPanel(new BorderLayout(0, 6));
		middle.setOpaque(false);
		middle.add(net, BorderLayout.NORTH);
		middle.add(bottom, BorderLayout.CENTER);

		JPanel footer = new JPanel(new GridLayout(0, 1, 0, 2));
		footer.setOpaque(false);
		for (JLabel label : new JLabel[]{details, todayValue})
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(UiFormat.MUTED_TEXT);
			footer.add(label);
		}

		card.add(top, BorderLayout.NORTH);
		card.add(middle, BorderLayout.CENTER);
		card.add(footer, BorderLayout.SOUTH);
		add(card);

		chartTitle.setFont(FontManager.getRunescapeBoldFont());
		chartTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		chartTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
		chartTitle.setAlignmentX(LEFT_ALIGNMENT);
		add(chartTitle);
		chart.setAlignmentX(LEFT_ALIGNMENT);
		add(chart);

		JPanel clearRow = new JPanel(new BorderLayout());
		clearRow.setOpaque(false);
		clearRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		clearRow.setAlignmentX(LEFT_ALIGNMENT);
		clearButton.setForeground(UiFormat.LOSS);
		clearButton.setFocusPainted(false);
		clearButton.addActionListener(e -> onClear.run());
		clearRow.add(clearButton, BorderLayout.CENTER);
		add(clearRow);
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

		clearButton.setEnabled(!readOnly && view.getTrips() > 0);
		revalidate();
		repaint();
	}
}
