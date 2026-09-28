package com.maggotkingtriptracker.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.maggotkingtriptracker.MaggotKingTripTrackerConfig;
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
	private boolean goal;
	private boolean trip;
	private boolean combine;
	private boolean onlyOnTrip = true;

	private final MaggotKingTripTrackerConfig config = new MaggotKingTripTrackerConfig()
	{
		@Override
		public boolean showGoalOverlay()
		{
			return goal;
		}

		@Override
		public boolean showTripOverlay()
		{
			return trip;
		}

		@Override
		public boolean combineOverlays()
		{
			return combine;
		}

		@Override
		public boolean overlayOnlyOnTrip()
		{
			return onlyOnTrip;
		}

		@Override
		public void setSelectedBoss(String bossId)
		{
		}
	};

	@Test
	public void offByDefaultAndOnlyDuringATrip()
	{
		PanelState inTrip = state(PanelState.Status.IN_TRIP);
		assertNull(render(TrackerOverlay.Kind.GOAL, inTrip));
		assertNull(render(TrackerOverlay.Kind.TRIP, inTrip));

		goal = true;
		trip = true;
		assertNotNull(render(TrackerOverlay.Kind.GOAL, inTrip));
		assertNotNull(render(TrackerOverlay.Kind.TRIP, inTrip));
		// Between trips they hide, unless told to stay
		assertNull(render(TrackerOverlay.Kind.GOAL, state(PanelState.Status.IDLE)));
		onlyOnTrip = false;
		assertNotNull(render(TrackerOverlay.Kind.TRIP, state(PanelState.Status.IDLE)));
	}

	@Test
	public void combinedGoesInTheGoalBox()
	{
		goal = true;
		trip = true;
		combine = true;
		PanelState inTrip = state(PanelState.Status.IN_TRIP);
		assertNotNull(render(TrackerOverlay.Kind.GOAL, inTrip));
		assertNull(render(TrackerOverlay.Kind.TRIP, inTrip));
	}

	@Test
	public void preview() throws Exception
	{
		goal = true;
		trip = true;
		BufferedImage image = new BufferedImage(480, 260, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(70, 90, 60));
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		// Separate boxes on the left, combined on the right. RuneLite's overlay renderer normally supplies the font
		// and the translucent background
		g.setFont(FontManager.getRunescapeFont());
		draw(g, overlay(TrackerOverlay.Kind.GOAL), 10, 10);
		draw(g, overlay(TrackerOverlay.Kind.TRIP), 10, 130);
		combine = true;
		draw(g, overlay(TrackerOverlay.Kind.GOAL), 170, 10);
		g.dispose();
		ImageIO.write(image, "PNG", new File("build/overlay-preview.png"));
	}

	private static void draw(Graphics2D g, TrackerOverlay overlay, int x, int y)
	{
		overlay.getPanelComponent().setBackgroundColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		Graphics2D at = (Graphics2D) g.create();
		at.translate(x, y);
		overlay.render(at);
		at.dispose();
	}

	private Dimension render(TrackerOverlay.Kind kind, PanelState state)
	{
		Graphics2D g = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB).createGraphics();
		Dimension size = new TrackerOverlay(null, kind, config, () -> state).render(g);
		g.dispose();
		return size;
	}

	private TrackerOverlay overlay(TrackerOverlay.Kind kind)
	{
		PanelState inTrip = state(PanelState.Status.IN_TRIP);
		return new TrackerOverlay(null, kind, config, () -> inTrip);
	}

	private static PanelState state(PanelState.Status status)
	{
		long now = System.currentTimeMillis();
		TripView trip = TripView.builder()
			.id("t")
			.startedAt(now - 3_600_000)
			.activeMs(2_985_000)
			.kills(21)
			.bossStat(new StatView("Stom / Eggs", "21 / 0", null))
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
			.status(status)
			.currentTrip(trip)
			.lifetime(lifetime)
			.goal(new GoalView(150, 59, 10_385_888, now, true))
			.killStartedAt(now - 73_000)
			.build();
	}
}
