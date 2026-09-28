package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.MaggotKingTripTrackerConfig;
import com.maggotkingtriptracker.OverlayGoalStat;
import com.maggotkingtriptracker.OverlayLootStat;
import com.maggotkingtriptracker.OverlayTripStat;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.view.GoalView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;

/**
 * An optional box on the game screen in the style of RuneLite's XP tracker box: the boss icon at the top left, up to
 * three rows beside it (a kill goal stat, a trip time and the trip's profit, each picked in the config) and the goal
 * progress bar underneath. It shows nothing about the boss or its mechanics, and drawing only formats a few numbers
 * from the latest panel state.
 */
public class TrackerOverlay extends OverlayPanel
{
	private static final int WIDTH = 150;
	private static final int ICON_GAP = 4;
	private static final int BAR_GAP = 2;
	private static final String NOT_AVAILABLE = "N/A";
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR.darker();

	private final MaggotKingTripTrackerConfig config;
	private final Supplier<PanelState> state;
	private final IntFunction<BufferedImage> icons;

	/**
	 * @param state the latest panel state; read on the client thread, where it is also produced
	 * @param icons item icon by id (the boss icon); may return null
	 */
	public TrackerOverlay(Plugin plugin, MaggotKingTripTrackerConfig config, Supplier<PanelState> state,
		IntFunction<BufferedImage> icons)
	{
		super(plugin);
		this.config = config;
		this.state = state;
		this.icons = icons;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(WIDTH, 0));
		panelComponent.setGap(new Point(0, BAR_GAP));
	}

	@Override
	public String getName()
	{
		return "BossTripTrackerOverlay";
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PanelState s = state.get();
		if (!config.showOverlay() || s == null || s.getLifetime() == null || s.getBoss() == null)
		{
			return null;
		}
		boolean onTrip = s.getStatus() == PanelState.Status.IN_TRIP || s.getStatus() == PanelState.Status.AFK_PAUSED;
		if (config.overlayOnlyOnTrip() && !onTrip)
		{
			return null;
		}

		long now = System.currentTimeMillis();
		GoalView goal = s.getGoal();
		TripView trip = s.getCurrentTrip();
		List<LineComponent> rows = new ArrayList<>(3);
		if (goal != null)
		{
			addGoalRow(rows, goal, config.overlayGoalRow(), now);
		}
		if (trip != null)
		{
			addTripRow(rows, s, trip, config.overlayTripRow(), now);
			addLootRow(rows, trip, config.overlayLootRow(), now);
		}
		boolean bar = config.overlayProgressBar() && goal != null;
		if (rows.isEmpty() && !bar)
		{
			return null;
		}

		BufferedImage icon = icons.apply(s.getBoss().getIconItemId());
		LayoutableRenderableEntity lines = stack(rows);
		if (lines != null)
		{
			panelComponent.getChildren().add(icon == null ? lines : SplitComponent.builder()
				.first(new ImageComponent(icon))
				.second(lines)
				.orientation(ComponentOrientation.HORIZONTAL)
				.gap(new Point(ICON_GAP, 0))
				.build());
		}
		if (bar)
		{
			panelComponent.getChildren().add(progressBar(goal));
		}
		return super.render(graphics);
	}

	/**
	 * The rows one above the other, beside the icon.
	 */
	private static LayoutableRenderableEntity stack(List<LineComponent> rows)
	{
		LayoutableRenderableEntity stacked = null;
		for (int i = rows.size() - 1; i >= 0; i--)
		{
			stacked = stacked == null ? rows.get(i) : SplitComponent.builder()
				.first(rows.get(i))
				.second(stacked)
				.orientation(ComponentOrientation.VERTICAL)
				.build();
		}
		return stacked;
	}

	private static void addGoalRow(List<LineComponent> rows, GoalView goal, OverlayGoalStat stat, long now)
	{
		// Time-based numbers are greyed while the goal clock is stopped, as in the panel
		Color clock = goal.isRunning() ? Color.WHITE : MUTED;
		switch (stat)
		{
			case KILLS_PER_HOUR:
				double killsPerHour = goal.killsPerHourAt(now);
				rows.add(line("KPH:", killsPerHour > 0 ? String.format(Locale.ROOT, "%.1f", killsPerHour) : NOT_AVAILABLE, clock));
				break;
			case TIME_TO_GOAL:
				Long toGoal = goal.msToGoalAt(now);
				rows.add(line("TTG:", goal.getRemaining() == 0 ? "Done" : toGoal != null ? GoalCard.timeToGoal(toGoal)
					: NOT_AVAILABLE, clock));
				break;
			case KILLS_DONE:
				rows.add(line("Done:", GoalCard.count(goal.getDone()), Color.WHITE));
				break;
			case KILLS_LEFT:
				rows.add(line("Left:", GoalCard.count(goal.getRemaining()), Color.WHITE));
				break;
			default:
				break;
		}
	}

	private static void addTripRow(List<LineComponent> rows, PanelState s, TripView trip, OverlayTripStat stat, long now)
	{
		switch (stat)
		{
			case CURRENT_KILL:
				Long start = s.getKillStartedAt();
				// Counts from the boss spawning, like the game's Fight duration; the last kill's time between kills
				rows.add(start != null ? line("Kill:", UiFormat.duration(now - start), Color.WHITE)
					: line("Last:", UiFormat.killTime(trip.getLastKillMs()), MUTED));
				break;
			case TRIP_TIME:
				rows.add(line("Trip:", UiFormat.duration(trip.activeMsAt(now)), s.getPauseText() != null ? MUTED : Color.WHITE));
				break;
			case KILLS:
				rows.add(line("Kills:", String.valueOf(trip.getKills()), Color.WHITE));
				break;
			case AVERAGE_KILL:
				rows.add(line("Avg:", UiFormat.killTime(trip.getAverageKillMs()), Color.WHITE));
				break;
			case PB:
				rows.add(line("PB:", UiFormat.killTime(trip.getFastestKillMs()), Color.WHITE));
				break;
			default:
				break;
		}
	}

	private static void addLootRow(List<LineComponent> rows, TripView trip, OverlayLootStat stat, long now)
	{
		switch (stat)
		{
			case NET_PROFIT:
				rows.add(line("Net:", UiFormat.gp(trip.getNetProfit()), UiFormat.profitColor(trip.getNetProfit())));
				break;
			case NET_GP_PER_HOUR:
				long rate = TripMath.gpPerHour(trip.getNetProfit(), trip.activeMsAt(now));
				rows.add(line("GP/hr:", UiFormat.gp(rate), UiFormat.profitColor(rate)));
				break;
			default:
				break;
		}
	}

	/**
	 * Kills done on the left, the percentage in the middle and the goal on the right.
	 */
	private static ProgressBarComponent progressBar(GoalView goal)
	{
		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMinimum(0);
		bar.setMaximum(Math.max(1, goal.getTarget()));
		bar.setValue(Math.min(goal.getDone(), goal.getTarget()));
		bar.setLeftLabel(GoalCard.count(goal.getDone()));
		bar.setRightLabel(GoalCard.count(goal.getTarget()));
		bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
		bar.setCenterLabel(String.format(Locale.ROOT, "%.1f%%", Math.min(100, goal.getDone() * 100.0 / Math.max(1, goal.getTarget()))));
		bar.setForegroundColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
		return bar;
	}

	private static LineComponent line(String left, String right, Color rightColor)
	{
		return LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColor)
			.build();
	}
}
