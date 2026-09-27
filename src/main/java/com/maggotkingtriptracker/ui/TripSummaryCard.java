package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.TripView;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Headline numbers for one trip: a time card (time, kills, average and fastest kill) and a profit card
 * (net, GP/hr, loot, costs, stomach/eggs, deaths). For a trip in progress, {@link #tick(long)} keeps the timer
 * and GP/hr live.
 */
class TripSummaryCard extends JPanel
{
	private final StatCell time = new StatCell("Time", false)
		.help("Time spent inside the lair this trip. Time outside (banking, logged out) isn't counted.");
	private final StatCell kills = new StatCell("Kills", false)
		.help("Kills this trip, from the game's kill-count message.");
	private final StatCell averageKill = new StatCell("Avg kill", false)
		.help("Average of the game's \"Fight duration\" for this trip's kills.");
	private final StatCell fastestKill = new StatCell("Fastest", false)
		.help("Shortest \"Fight duration\" this trip.");

	private final StatCell net = new StatCell("Net profit", true)
		.help("Loot minus costs (supplies, dropped items and death costs), at the GE prices recorded at the time.");
	private final StatCell gpPerHour = new StatCell("GP/hr", true)
		.help("Net profit per hour of time inside the lair.");
	private final StatCell loot = new StatCell("Loot", false)
		.help("GE value of everything received, including overflow picked up from the ground. Tarnished drops count"
			+ " once polished.");
	private final StatCell costs = new StatCell("Costs", false)
		.help("Supplies used + items dropped and left behind + death costs. Hover the value for the split.");
	private final StatCell split = new StatCell("Stom / Eggs", false)
		.help("Kills where you chose Open-stomach / Take-eggs on the corpse.");
	private final StatCell deaths = new StatCell("Deaths", false)
		.help("Deaths in the lair this trip.");

	private TripView trip;

	TripSummaryCard()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);

		JPanel timeCard = card(new GridLayout(1, 4, 4, 0), time, kills, averageKill, fastestKill);
		JPanel profitCard = card(new GridLayout(2, 3, 4, 3), net, gpPerHour, loot, costs, split, deaths);

		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setAlignmentX(LEFT_ALIGNMENT);
		gap.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

		add(timeCard);
		add(gap);
		add(profitCard);
	}

	private static JPanel card(GridLayout layout, StatCell... cells)
	{
		JPanel card = new JPanel(layout);
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		card.setAlignmentX(LEFT_ALIGNMENT);
		for (StatCell cell : cells)
		{
			card.add(cell);
		}
		return card;
	}

	void setTrip(TripView trip, long now)
	{
		this.trip = trip;
		kills.setValue(String.valueOf(trip.getKills()));
		averageKill.setValue(UiFormat.killTime(trip.getAverageKillMs()));
		fastestKill.setValue(UiFormat.killTime(trip.getFastestKillMs()));

		net.setValue(UiFormat.gp(trip.getNetProfit()), UiFormat.profitColor(trip.getNetProfit()), UiFormat.fullGp(trip.getNetProfit()));
		loot.setValue(UiFormat.gp(trip.getLootValue()), ColorScheme.LIGHT_GRAY_COLOR, UiFormat.fullGp(trip.getLootValue()));
		long totalCosts = trip.getSupplyCost() + trip.getDroppedCost() + trip.getDeathCost();
		costs.setValue(UiFormat.gp(totalCosts), ColorScheme.LIGHT_GRAY_COLOR, "<html>Supplies: " + UiFormat.fullGp(trip.getSupplyCost())
			+ "<br>Dropped: " + UiFormat.fullGp(trip.getDroppedCost())
			+ "<br>Deaths: " + UiFormat.fullGp(trip.getDeathCost()) + "</html>");
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
