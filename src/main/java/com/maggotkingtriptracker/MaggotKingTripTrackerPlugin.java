package com.maggotkingtriptracker;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.maggotkingtriptracker.diagnostic.DiagnosticRecorder;
import com.maggotkingtriptracker.persistence.HistoryStore;
import com.maggotkingtriptracker.pricing.PriceService;
import com.maggotkingtriptracker.tracking.TripTracker;
import com.maggotkingtriptracker.ui.TrackerPanel;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Maggot King Trip Tracker",
	description = "Tracks loot, supplies and profit per Maggot King trip with per-account history",
	tags = {"maggot", "king", "vampyrium", "loot", "profit", "supplies", "trip", "boss"},
	internalName = "maggot-king-trip-tracker"
)
public class MaggotKingTripTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Gson gson;

	@Inject
	private MaggotKingTripTrackerConfig config;

	private DiagnosticRecorder diagnosticRecorder;
	private ScheduledExecutorService executor;
	private TripTracker tripTracker;
	private TrackerPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp() throws Exception
	{
		DiagnosticRecorder recorder = new DiagnosticRecorder(client, itemManager, this::getPluginDirectory);
		diagnosticRecorder = recorder;
		eventBus.register(recorder);
		boolean diagnosticMode = config.diagnosticMode();
		clientThread.invokeLater(() -> recorder.setEnabled(diagnosticMode));

		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread thread = new Thread(r, "maggot-king-trip-tracker-io");
			thread.setDaemon(true);
			return thread;
		});

		TrackerPanel trackerPanel = new TrackerPanel(itemManager, this::deleteTrip, this::clearHistory);
		panel = trackerPanel;

		HistoryStore store = new HistoryStore(gson, this::getPluginDirectory, executor);
		TripTracker tracker = new TripTracker(client, clientThread, config, new PriceService(itemManager), store,
			gson, executor, state -> SwingUtilities.invokeLater(() -> trackerPanel.update(state)));
		tripTracker = tracker;
		eventBus.register(tracker);
		clientThread.invokeLater(tracker::start);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Maggot King Trip Tracker")
			.icon(icon)
			.priority(7)
			.panel(trackerPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		log.debug("Maggot King Trip Tracker started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(tripTracker);
		tripTracker.shutDown();
		tripTracker = null;

		eventBus.unregister(diagnosticRecorder);
		diagnosticRecorder.shutDown();
		diagnosticRecorder = null;

		executor.shutdownNow();
		executor = null;

		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
		panel.shutDown();
		panel = null;

		log.debug("Maggot King Trip Tracker stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MaggotKingTripTrackerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("diagnosticMode".equals(event.getKey()))
		{
			boolean diagnosticMode = config.diagnosticMode();
			DiagnosticRecorder recorder = diagnosticRecorder;
			clientThread.invokeLater(() -> recorder.setEnabled(diagnosticMode));
		}
		else if ("showCurrentValue".equals(event.getKey()))
		{
			TripTracker tracker = tripTracker;
			clientThread.invokeLater(tracker::refreshView);
		}
	}

	private void deleteTrip(String tripId)
	{
		TripTracker tracker = tripTracker;
		if (tracker != null)
		{
			clientThread.invokeLater(() -> tracker.deleteTrip(tripId));
		}
	}

	private void clearHistory()
	{
		TripTracker tracker = tripTracker;
		if (tracker != null)
		{
			clientThread.invokeLater(tracker::clearHistory);
		}
	}

	@Provides
	MaggotKingTripTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MaggotKingTripTrackerConfig.class);
	}
}
