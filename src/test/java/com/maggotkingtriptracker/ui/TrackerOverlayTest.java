package com.maggotkingtriptracker.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.maggotkingtriptracker.MaggotKingTripTrackerConfig;
import com.maggotkingtriptracker.OverlayGoalStat;
import com.maggotkingtriptracker.OverlayLootStat;
import com.maggotkingtriptracker.OverlayTripStat;
import com.maggotkingtriptracker.boss.MaggotKingBoss;
import com.maggotkingtriptracker.view.GoalView;
import com.maggotkingtriptracker.view.LifetimeView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.StatView;
import com.maggotkingtriptracker.view.TripView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import org.junit.Test;

public class TrackerOverlayTest
{
	private boolean showGoal;
	private boolean showTrip;
	private boolean showLoot;
	private boolean onlyOnTrip = true;
	private boolean bar = true;
	private OverlayGoalStat goalRow = OverlayGoalStat.KILLS_PER_HOUR;
	private OverlayTripStat tripRow = OverlayTripStat.CURRENT_KILL;
	private OverlayLootStat lootRow = OverlayLootStat.NET_PROFIT;

	private final MaggotKingTripTrackerConfig config = new MaggotKingTripTrackerConfig()
	{
		@Override
		public boolean overlayShowGoal()
		{
			return showGoal;
		}

		@Override
		public boolean overlayShowTrip()
		{
			return showTrip;
		}

		@Override
		public boolean overlayShowLoot()
		{
			return showLoot;
		}

		@Override
		public boolean overlayOnlyOnTrip()
		{
			return onlyOnTrip;
		}

		@Override
		public boolean overlayProgressBar()
		{
			return bar;
		}

		@Override
		public OverlayGoalStat overlayGoalRow()
		{
			return goalRow;
		}

		@Override
		public OverlayTripStat overlayTripRow()
		{
			return tripRow;
		}

		@Override
		public OverlayLootStat overlayLootRow()
		{
			return lootRow;
		}

		@Override
		public void setSelectedBoss(String bossId)
		{
		}
	};

	@Test
	public void offByDefaultAndOnlyDuringATrip()
	{
		assertNull(render(state(PanelState.Status.IN_TRIP, true)));
		showAll();
		assertNotNull(render(state(PanelState.Status.IN_TRIP, true)));
		assertNull(render(state(PanelState.Status.IDLE, true)));
		onlyOnTrip = false;
		assertNotNull(render(state(PanelState.Status.IDLE, true)));
	}

	@Test
	public void boxOnlyAppearsWithSomethingToShow()
	{
		showGoal = true;
		bar = false;
		// Just the goal row
		assertNotNull(render(state(PanelState.Status.IN_TRIP, true)));
		// Without a goal the goal row and bar are hidden, and nothing is left
		assertNull(render(state(PanelState.Status.IN_TRIP, false)));
		// The progress bar alone is enough
		bar = true;
		assertNotNull(render(state(PanelState.Status.IN_TRIP, true)));
		// A trip row shows without a goal
		showTrip = true;
		assertNotNull(render(state(PanelState.Status.IN_TRIP, false)));
	}

	private void showAll()
	{
		showGoal = true;
		showTrip = true;
		showLoot = true;
	}

	@Test
	public void preview() throws Exception
	{
		showAll();
		BufferedImage image = new BufferedImage(340, 110, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(70, 90, 60));
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.setFont(FontManager.getRunescapeFont());
		// All three rows on the left (KPH, current kill, net profit, bar); goal and trip only (TTG, trip time) in the
		// middle, the size of RuneLite's XP tracker box
		draw(g, 10, 10);
		showLoot = false;
		goalRow = OverlayGoalStat.TIME_TO_GOAL;
		tripRow = OverlayTripStat.TRIP_TIME;
		draw(g, 160, 10);
		g.dispose();
		ImageIO.write(image, "PNG", new File("build/overlay-preview.png"));
	}

	private void draw(Graphics2D g, int x, int y)
	{
		TrackerOverlay overlay = overlay(state(PanelState.Status.IN_TRIP, true));
		// RuneLite's overlay renderer normally supplies the font and the translucent background
		overlay.getPanelComponent().setBackgroundColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		Graphics2D at = (Graphics2D) g.create();
		at.translate(x, y);
		// PanelComponent sizes itself from the previous frame's layout, so draw a first frame off-screen
		Graphics2D scratch = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
		overlay.render(scratch);
		scratch.dispose();
		overlay.render(at);
		at.dispose();
	}

	private Dimension render(PanelState state)
	{
		Graphics2D g = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB).createGraphics();
		g.setFont(FontManager.getRunescapeFont());
		Dimension size = overlay(state).render(g);
		g.dispose();
		return size;
	}

	private TrackerOverlay overlay(PanelState state)
	{
		return new TrackerOverlay(null, config, () -> state, TrackerOverlayTest::placeholderIcon);
	}

	private static BufferedImage placeholderIcon(int itemId)
	{
		BufferedImage icon = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setColor(new Color(150, 100, 70));
		g.fillOval(6, 1, 24, 30);
		g.dispose();
		return icon;
	}

	private static PanelState state(PanelState.Status status, boolean withGoal)
	{
		long now = System.currentTimeMillis();
		TripView trip = TripView.builder()
			.id("t")
			.startedAt(now - 3_600_000)
			.activeMs(2_985_000)
			.kills(21)
			.bossStat(new StatView("Stom / Eggs", "21 / 0", null))
			.netProfit(-1_240_000)
			.averageKillMs(131_200L)
			.fastestKillMs(104_400L)
			.lastKillMs(122_000L)
			.loot(Collections.emptyList())
			.supplies(Collections.emptyList())
			.dropped(Collections.emptyList())
			.supplyCategories(Collections.emptyList())
			.build();
		LifetimeView lifetime = LifetimeView.builder().netPerTrip(Collections.<Long>emptyList()).build();
		return PanelState.builder()
			.boss(new MaggotKingBoss())
			.status(status)
			.currentTrip(trip)
			.lifetime(lifetime)
			.goal(withGoal ? new GoalView(392, 163, 24_247_000, now, true) : null)
			.killStartedAt(now - 73_000)
			.build();
	}
}
