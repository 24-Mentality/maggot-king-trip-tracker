package com.maggotkingtriptracker;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.maggotkingtriptracker.diagnostic.DiagnosticRecorder;
import com.maggotkingtriptracker.persistence.HistoryStore;
import com.maggotkingtriptracker.pricing.PriceService;
import com.maggotkingtriptracker.tracking.TripTracker;
import com.maggotkingtriptracker.ui.PanelActions;
import com.maggotkingtriptracker.ui.TrackerPanel;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.Notifier;
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
import net.runelite.client.util.Filepath;
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
	private Notifier notifier;

	@Inject
	private MaggotKingTripTrackerConfig config;

	private DiagnosticRecorder diagnosticRecorder;
	private ScheduledExecutorService executor;
	private HistoryStore store;
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

		TrackerPanel trackerPanel = new TrackerPanel(itemManager, new Actions());
		panel = trackerPanel;

		store = new HistoryStore(gson, this::getPluginDirectory, executor);
		TripTracker tracker = new TripTracker(client, clientThread, config, new PriceService(itemManager), store,
			gson, executor, state -> SwingUtilities.invokeLater(() -> trackerPanel.update(state)),
			message -> notifier.notify(config.alertNotification(), message),
			this::lairEntered);
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
		store = null;

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

	/**
	 * Called on the client thread when the player enters the lair.
	 */
	private void lairEntered()
	{
		if (!config.openPanelOnEntry())
		{
			return;
		}
		NavigationButton button = navigationButton;
		TrackerPanel trackerPanel = panel;
		SwingUtilities.invokeLater(() ->
		{
			if (button != null && trackerPanel != null)
			{
				clientToolbar.openPanel(button);
				trackerPanel.showTripTab();
			}
		});
	}

	/**
	 * Panel buttons. Runs on the Swing thread; file dialogs open here, IO runs on the executor and
	 * history access on the client thread.
	 */
	private class Actions implements PanelActions
	{
		@Override
		public void deleteTrip(String tripId)
		{
			TripTracker tracker = tripTracker;
			clientThread.invokeLater(() -> tracker.deleteTrip(tripId));
		}

		@Override
		public void clearHistory()
		{
			TripTracker tracker = tripTracker;
			clientThread.invokeLater(tracker::clearHistory);
		}

		@Override
		public void setGoal(int target)
		{
			TripTracker tracker = tripTracker;
			clientThread.invokeLater(() -> tracker.setGoal(target));
		}

		@Override
		public void resetGoal()
		{
			TripTracker tracker = tripTracker;
			clientThread.invokeLater(tracker::resetGoal);
		}

		@Override
		public void exportCsv()
		{
			export("Export trips", "maggot-king-trips.csv", "CSV files", "csv", TripTracker::exportCsv);
		}

		@Override
		public void exportJson()
		{
			export("Export history", "maggot-king-history.json", "JSON files", "json", TripTracker::exportJson);
		}

		@Override
		public void importJson()
		{
			List<Filepath> chosen = new Filepath.Chooser()
				.setIsOpen()
				.setAcceptsFiles()
				.setDialogTitle("Import history")
				.addExtensionFilter("JSON files", "json")
				.showDialog(panel);
			if (chosen.isEmpty())
			{
				return;
			}

			TripTracker tracker = tripTracker;
			TrackerPanel trackerPanel = panel;
			store.readHistoryFile(chosen.get(0), (imported, error) ->
			{
				if (error != null)
				{
					SwingUtilities.invokeLater(() -> trackerPanel.showMessage("Import history",
						"That file couldn't be read as a Maggot King Trip Tracker export.", true));
					return;
				}
				clientThread.invokeLater(() ->
				{
					String description = tracker.describeImport(imported);
					SwingUtilities.invokeLater(() ->
					{
						if (description.startsWith("!"))
						{
							trackerPanel.showMessage("Import history", description.substring(1), true);
						}
						else if (trackerPanel.confirm("Import history", description))
						{
							clientThread.invokeLater(() -> tracker.importHistory(imported));
						}
					});
				});
			});
		}

		private void export(String title, String fileName, String filterName, String extension,
			Function<TripTracker, String> content)
		{
			List<Filepath> chosen = new Filepath.Chooser()
				.setIsSave()
				.setDialogTitle(title)
				.setFileName(fileName)
				.addExtensionFilter(filterName, extension)
				.setDefaultExtension(extension)
				.showDialog(panel);
			if (chosen.isEmpty())
			{
				return;
			}

			Filepath file = chosen.get(0);
			TripTracker tracker = tripTracker;
			TrackerPanel trackerPanel = panel;
			HistoryStore historyStore = store;
			clientThread.invokeLater(() ->
			{
				String data = content.apply(tracker);
				if (data == null)
				{
					SwingUtilities.invokeLater(() -> trackerPanel.showMessage(title, "Log in first.", true));
					return;
				}
				historyStore.writeFile(file, data, error -> SwingUtilities.invokeLater(() -> trackerPanel.showMessage(title,
					error == null ? "Saved to " + file.getFileName() : "Couldn't save the file: " + error.getMessage(),
					error != null)));
			});
		}
	}

	@Provides
	MaggotKingTripTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MaggotKingTripTrackerConfig.class);
	}
}
