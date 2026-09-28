package com.maggotkingtriptracker.ui;

import com.maggotkingtriptracker.GoalOverlayLine;
import com.maggotkingtriptracker.MaggotKingTripTrackerConfig;
import com.maggotkingtriptracker.TripOverlayLine;
import com.maggotkingtriptracker.view.GoalView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * An optional box on the game screen with the kill goal or the trip's times, the same numbers as the panel. It
 * shows nothing about the boss or its mechanics. Everything is worked out when the panel state is built; drawing
 * only formats a few numbers.
 */
public class TrackerOverlay extends OverlayPanel
{
	public enum Kind
	{
		GOAL,
		TRIP,
	}

	private static final String NOT_AVAILABLE = "N/A";
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR.darker();

	private final Kind kind;
	private final MaggotKingTripTrackerConfig config;
	private final Supplier<PanelState> state;

	/**
	 * @param state the latest panel state; read on the client thread, where it is also produced
	 */
	public TrackerOverlay(Plugin plugin, Kind kind, MaggotKingTripTrackerConfig config, Supplier<PanelState> state)
	{
		super(plugin);
		this.kind = kind;
		this.config = config;
		this.state = state;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(140, 0));
	}

	/**
	 * RuneLite saves an overlay's position under its name, which defaults to the class name. The two boxes share
	 * this class, so each needs its own name to be moved separately.
	 */
	@Override
	public String getName()
	{
		return kind == Kind.GOAL ? "BossTripTrackerGoalOverlay" : "BossTripTrackerTripOverlay";
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PanelState s = state.get();
		if (s == null || s.getLifetime() == null)
		{
			return null;
		}
		boolean onTrip = s.getStatus() == PanelState.Status.IN_TRIP || s.getStatus() == PanelState.Status.AFK_PAUSED;
		if (config.overlayOnlyOnTrip() && !onTrip)
		{
			return null;
		}

		Set<GoalOverlayLine> goalLines = config.goalOverlayLines();
		Set<TripOverlayLine> tripLines = config.tripOverlayLines();
		boolean goalShown = config.showGoalOverlay() && s.getGoal() != null && !goalLines.isEmpty();
		boolean tripShown = config.showTripOverlay() && s.getCurrentTrip() != null && !tripLines.isEmpty();
		// Combined, both go in the goal overlay's box and the trip overlay draws nothing
		boolean combined = config.combineOverlays() && goalShown && tripShown;
		long now = System.currentTimeMillis();

		if (kind == Kind.GOAL)
		{
			if (!goalShown)
			{
				return null;
			}
			addGoal(s.getGoal(), goalLines, now);
			if (combined)
			{
				addTrip(s, tripLines, now);
			}
		}
		else
		{
			if (!tripShown || combined)
			{
				return null;
			}
			addTrip(s, tripLines, now);
		}
		return super.render(graphics);
	}

	private void addGoal(GoalView goal, Set<GoalOverlayLine> lines, long now)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(String.format(Locale.ROOT, "Goal: %,d kills", goal.getTarget()))
			.build());
		// Time-based numbers are greyed while the goal clock is stopped, as in the panel
		Color clock = goal.isRunning() ? Color.WHITE : MUTED;
		double killsPerHour = goal.killsPerHourAt(now);
		Long toGoal = goal.msToGoalAt(now);
		if (lines.contains(GoalOverlayLine.KILLS_PER_HOUR))
		{
			line("KPH:", killsPerHour > 0 ? String.format(Locale.ROOT, "%.1f", killsPerHour) : NOT_AVAILABLE, clock);
		}
		if (lines.contains(GoalOverlayLine.KILLS_DONE))
		{
			line("Kills done:", GoalCard.count(goal.getDone()), Color.WHITE);
		}
		if (lines.contains(GoalOverlayLine.KILLS_LEFT))
		{
			line("Kills left:", GoalCard.count(goal.getRemaining()), Color.WHITE);
		}
		if (lines.contains(GoalOverlayLine.TIME_TO_GOAL))
		{
			line("TTG:", goal.getRemaining() == 0 ? "Done" : toGoal != null ? GoalCard.timeToGoal(toGoal) : NOT_AVAILABLE, clock);
		}
		if (lines.contains(GoalOverlayLine.PROGRESS_BAR))
		{
			ProgressBarComponent bar = new ProgressBarComponent();
			bar.setMinimum(0);
			bar.setMaximum(Math.max(1, goal.getTarget()));
			bar.setValue(Math.min(goal.getDone(), goal.getTarget()));
			bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
			bar.setCenterLabel(String.format(Locale.ROOT, "%.1f%%",
				Math.min(100, goal.getDone() * 100.0 / Math.max(1, goal.getTarget()))));
			bar.setForegroundColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			panelComponent.getChildren().add(bar);
		}
	}

	private void addTrip(PanelState s, Set<TripOverlayLine> lines, long now)
	{
		TripView trip = s.getCurrentTrip();
		boolean live = s.getStatus() == PanelState.Status.IN_TRIP || s.getStatus() == PanelState.Status.AFK_PAUSED;
		panelComponent.getChildren().add(TitleComponent.builder().text(live ? "Trip" : "Last trip").build());
		if (lines.contains(TripOverlayLine.TIME))
		{
			line("Time:", UiFormat.duration(trip.activeMsAt(now)), s.getPauseText() != null ? MUTED : Color.WHITE);
		}
		if (lines.contains(TripOverlayLine.KILLS))
		{
			line("Kills:", String.valueOf(trip.getKills()), Color.WHITE);
		}
		if (lines.contains(TripOverlayLine.AVERAGE_KILL))
		{
			line("Avg kill:", UiFormat.killTime(trip.getAverageKillMs()), Color.WHITE);
		}
		if (lines.contains(TripOverlayLine.PB))
		{
			line("PB:", UiFormat.killTime(trip.getFastestKillMs()), Color.WHITE);
		}
		if (lines.contains(TripOverlayLine.CURRENT_KILL))
		{
			Long start = s.getKillStartedAt();
			if (start != null)
			{
				// Counts from the boss spawning, like the game's Fight duration
				line("Current:", UiFormat.duration(now - start), Color.WHITE);
			}
			else
			{
				line("Last kill:", UiFormat.killTime(trip.getLastKillMs()), MUTED);
			}
		}
	}

	private void line(String left, String right, Color rightColor)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColor)
			.build());
	}
}
