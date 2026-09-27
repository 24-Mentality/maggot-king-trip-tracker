package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.TripView;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Headline numbers for one trip in a 3 x 3 grid. For a trip in progress, {@link #tick(long)} keeps the timer
 * and GP/hr live.
 */
class TripSummaryCard extends JPanel
{
	private final StatCell time = new StatCell("Time", false);
	private final StatCell kills = new StatCell("Kills", false);
	private final StatCell averageKill = new StatCell("Avg kill", false);
	private final StatCell loot = new StatCell("Loot", false);
	private final StatCell costs = new StatCell("Costs", false);
	private final StatCell net = new StatCell("Net", true);
	private final StatCell gpPerHour = new StatCell("GP/hr", true);
	private final StatCell split = new StatCell("Stom / Eggs", false);
	private final StatCell deaths = new StatCell("Deaths", false);

	private TripView trip;

	TripSummaryCard()
	{
		setLayout(new GridLayout(3, 3, 4, 3));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

		for (StatCell cell : new StatCell[]{time, kills, averageKill, loot, costs, net, gpPerHour, split, deaths})
		{
			add(cell);
		}
	}

	void setTrip(TripView trip, long now)
	{
		this.trip = trip;
		kills.setValue(String.valueOf(trip.getKills()));
		averageKill.setValue(UiFormat.killTime(trip.getAverageKillMs()));
		loot.setValue(UiFormat.gp(trip.getLootValue()), ColorScheme.LIGHT_GRAY_COLOR, UiFormat.fullGp(trip.getLootValue()));

		long totalCosts = trip.getSupplyCost() + trip.getDroppedCost() + trip.getDeathCost();
		costs.setValue(UiFormat.gp(totalCosts), ColorScheme.LIGHT_GRAY_COLOR, "<html>Supplies: " + UiFormat.fullGp(trip.getSupplyCost())
			+ "<br>Dropped: " + UiFormat.fullGp(trip.getDroppedCost())
			+ "<br>Deaths: " + UiFormat.fullGp(trip.getDeathCost()) + "</html>");
		net.setValue(UiFormat.gp(trip.getNetProfit()), UiFormat.profitColor(trip.getNetProfit()), UiFormat.fullGp(trip.getNetProfit()));
		split.setValue(trip.getStomachKills() + " / " + trip.getEggKills());
		deaths.setValue(trip.getDeaths() + (trip.isPet() ? " · Pet!" : ""));
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
		gpPerHour.setValue(UiFormat.gp(rate), UiFormat.profitColor(rate), UiFormat.fullGp(rate) + " net per hour in the lair");
	}
}
