package com.maggotkingtriptracker.ui;

import static org.junit.Assert.assertTrue;
import com.maggotkingtriptracker.model.TripEndReason;
import com.maggotkingtriptracker.view.DrynessView;
import com.maggotkingtriptracker.view.GoalView;
import com.maggotkingtriptracker.view.ItemView;
import com.maggotkingtriptracker.view.LifetimeView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.PolishView;
import com.maggotkingtriptracker.view.SupplyCategory;
import com.maggotkingtriptracker.view.TripView;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * Lays out the whole panel at the narrowest sidebar width with worst-case values and checks that no label is
 * cut off or wraps.
 */
public class PanelFitTest
{
	/**
	 * The sidebar gives a non-scrolling PluginPanel 242px, minus room for our own vertical scrollbar.
	 */
	private static final int WIDTH = 242 - 17;

	@Test
	public void everyLabelFitsOnAllTabs() throws Exception
	{
		List<String> problems = new ArrayList<>();
		SwingUtilities.invokeAndWait(() ->
		{
			TrackerPanel panel = new TrackerPanel(null, new NoActions());
			panel.update(worstCaseState(PanelState.Status.IN_TRIP, false));
			for (int tab = 0; tab < 3; tab++)
			{
				panel.selectTab(tab);
				check(panel, "tab " + tab, problems);
			}

			// Paused states change button and status text
			panel.update(worstCaseState(PanelState.Status.AFK_PAUSED, true));
			panel.selectTab(0);
			check(panel, "paused", problems);
			panel.shutDown();
		});
		assertTrue("Labels that don't fit:\n" + String.join("\n", problems), problems.isEmpty());
	}

	private static void check(TrackerPanel panel, String where, List<String> problems)
	{
		panel.setSize(WIDTH, 4000);
		// Twice: the first pass settles nested preferred sizes
		layout(panel);
		layout(panel);
		findLabels(panel, where, problems);
	}

	private static void layout(Component component)
	{
		component.invalidate();
		component.doLayout();
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				layout(child);
			}
		}
	}

	private static void findLabels(Component component, String where, List<String> problems)
	{
		if (!component.isVisible())
		{
			return;
		}
		if (component instanceof JLabel)
		{
			JLabel label = (JLabel) component;
			String text = label.getText();
			if (text != null && !text.isEmpty() && label.getWidth() > 0
				&& label.getPreferredSize().width > label.getWidth())
			{
				problems.add(where + ": \"" + text.replaceAll("<[^>]+>", "") + "\" needs "
					+ label.getPreferredSize().width + "px, has " + label.getWidth() + "px");
			}
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				findLabels(child, where, problems);
			}
		}
	}

	private static PanelState worstCaseState(PanelState.Status status, boolean paused)
	{
		TripView trip = TripView.builder()
			.id("trip")
			.startedAt(1_790_000_000_000L)
			.endedAt(1_790_045_000_000L)
			.endReason(TripEndReason.TELEPORT)
			.activeMs((12 * 3600 + 34 * 60 + 56) * 1000L)
			.kills(999)
			.stomachKills(999)
			.eggKills(999)
			.deaths(99)
			.pet(true)
			.lootValue(123_456_789_000L)
			.supplyCost(99_999_999_000L)
			.droppedCost(9_999_999_000L)
			.deathCost(9_999_999_000L)
			.netProfit(-12_400_000_000L)
			.averageKillMs(599_900L)
			.fastestKillMs(599_900L)
			.loot(Collections.<ItemView>emptyList())
			.supplies(Collections.<ItemView>emptyList())
			.dropped(Collections.<ItemView>emptyList())
			.supplyCategories(Arrays.asList(
				new SupplyCategory("Charges", 99_999_999_000L),
				new SupplyCategory("Runes", 99_999_999_000L),
				new SupplyCategory("Potions", 99_999_999_000L),
				new SupplyCategory("Food", 99_999_999_000L),
				new SupplyCategory("Other", 99_999_999_000L)))
			.build();

		List<DrynessView.Unique> uniques = Arrays.asList(
			new DrynessView.Unique(ItemID.ELDER_VENATOR_FANG, "Elder venator fang", 36.31, Arrays.asList(12_345, 12_346)),
			new DrynessView.Unique(ItemID.CRIMSON_KISTEN, "Crimson kisten", 23.74, Collections.singletonList(12_345)));
		List<DrynessView.EggTier> eggs = new ArrayList<>();
		for (int id : new int[]{ItemID.MAGGOT_EGG, ItemID.SICKLY_MAGGOT_EGG, ItemID.WARM_MAGGOT_EGG,
			ItemID.PULSATING_MAGGOT_EGG, ItemID.WRIGGLING_MAGGOT_EGG, ItemID.WRITHING_MAGGOT_EGG})
		{
			eggs.add(new DrynessView.EggTier(id, "Pulsating maggot egg", 999, 99, 1 / 3000.0));
		}
		// Realistic but large: KC 12,345 with 99 uniques
		DrynessView dryness = new DrynessView(12_345, 1_234, 0.0024, uniques, 3.53, 9, eggs, 0.9999, 9,
			12_345, 11_111, 2_565, 99, 60.04,
			new DrynessView.AllTime(12_345, 12_345, 1_785_447_588_633L, 60, 39, 9));

		List<PolishView> polish = Collections.singletonList(new PolishView(ItemID.TARNISHED_NECKLACE, "Tarnished necklace",
			999, Collections.singletonList(new ItemView(ItemID.DIAMOND_NECKLACE, "Diamond necklace", 999, 0, false, false,
			false, null, 0, null))));

		LifetimeView lifetime = LifetimeView.builder()
			.trips(9_999)
			.kills(99_999)
			.stomachKills(99_999)
			.eggKills(99_999)
			.deaths(9_999)
			.pets(99)
			.activeMs(999L * 3600 * 1000)
			.lootValue(123_456_789_000L)
			.supplyCost(99_999_999_000L)
			.droppedCost(9_999_999_000L)
			.deathCost(9_999_999_000L)
			.netProfit(-12_400_000_000L)
			.averageKillMs(599_900L)
			.lootValueToday(123_456_789_000L)
			.netPerTrip(Arrays.asList(-12_400_000_000L, 5_000_000L))
			.dryness(dryness)
			.polish(polish)
			.build();

		// 1 kill per hour for 12,000 hours: KPH and the time to goal are at their longest
		GoalView goal = new GoalView(99_999, 12_345, 12_345L * 3600 * 1000, System.currentTimeMillis(), !paused);
		return new PanelState(status, trip, Collections.singletonList(trip), lifetime, goal, paused, false);
	}

	private static class NoActions implements PanelActions
	{
		@Override
		public void deleteTrip(String tripId)
		{
		}

		@Override
		public void clearHistory()
		{
		}

		@Override
		public void exportCsv()
		{
		}

		@Override
		public void exportJson()
		{
		}

		@Override
		public void importJson()
		{
		}

		@Override
		public void setGoal(int target)
		{
		}

		@Override
		public void resetGoal()
		{
		}

		@Override
		public void togglePause()
		{
		}
	}
}
