package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.TripView;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Headline numbers for one trip. For a trip in progress, {@link #tick(long)} keeps the timer and GP/hr live.
 */
class TripSummaryCard extends JPanel
{
	private final StatCell time = new StatCell("Trip time", false);
	private final StatCell kills = new StatCell("Kills", false);
	private final StatCell loot = new StatCell("Loot", false);
	private final StatCell supplies = new StatCell("Costs", false);
	private final StatCell net = new StatCell("Net profit", true);
	private final StatCell gpPerHour = new StatCell("GP/hr", false);
	private final StatCell averageKill = new StatCell("Avg kill", false);
	private final JLabel details = new JLabel();

	private TripView trip;

	TripSummaryCard()
	{
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel top = new JPanel(new GridLayout(2, 2, 6, 6));
		top.setOpaque(false);
		top.add(time);
		top.add(kills);
		top.add(loot);
		top.add(supplies);

		JPanel bottom = new JPanel(new GridLayout(1, 2, 6, 6));
		bottom.setOpaque(false);
		bottom.add(gpPerHour);
		bottom.add(averageKill);

		JPanel middle = new JPanel(new BorderLayout(0, 6));
		middle.setOpaque(false);
		middle.add(net, BorderLayout.NORTH);
		middle.add(bottom, BorderLayout.CENTER);

		details.setFont(FontManager.getRunescapeSmallFont());
		details.setForeground(UiFormat.MUTED_TEXT);

		add(top, BorderLayout.NORTH);
		add(middle, BorderLayout.CENTER);
		add(details, BorderLayout.SOUTH);
	}

	void setTrip(TripView trip, long now)
	{
		this.trip = trip;
		kills.setValue(String.valueOf(trip.getKills()));
		loot.setValue(UiFormat.gp(trip.getLootValue()), ColorScheme.LIGHT_GRAY_COLOR, UiFormat.fullGp(trip.getLootValue()));

		long costs = trip.getSupplyCost() + trip.getDroppedCost() + trip.getDeathCost();
		supplies.setValue(UiFormat.gp(costs), ColorScheme.LIGHT_GRAY_COLOR, "<html>Supplies: " + UiFormat.fullGp(trip.getSupplyCost())
			+ "<br>Dropped: " + UiFormat.fullGp(trip.getDroppedCost())
			+ "<br>Deaths: " + UiFormat.fullGp(trip.getDeathCost()) + "</html>");
		net.setValue(UiFormat.gp(trip.getNetProfit()), UiFormat.profitColor(trip.getNetProfit()), UiFormat.fullGp(trip.getNetProfit()));
		averageKill.setValue(UiFormat.killTime(trip.getAverageKillMs()));

		StringBuilder sb = new StringBuilder()
			.append("Stomach ").append(trip.getStomachKills())
			.append(" · Eggs ").append(trip.getEggKills());
		if (trip.getDeaths() > 0)
		{
			sb.append(" · Deaths ").append(trip.getDeaths());
		}
		if (trip.isPet())
		{
			sb.append(" · Pet!");
		}
		details.setText(sb.toString());
		tick(now);
	}

	void tick(long now)
	{
		if (trip == null)
		{
			return;
		}
		long activeMs = trip.activeMsAt(now);
		time.setValue(UiFormat.duration(activeMs));
		long rate = TripMath.gpPerHour(trip.getNetProfit(), activeMs);
		gpPerHour.setValue(UiFormat.gp(rate), UiFormat.profitColor(rate), UiFormat.fullGp(rate) + " per hour in the lair");
	}
}
