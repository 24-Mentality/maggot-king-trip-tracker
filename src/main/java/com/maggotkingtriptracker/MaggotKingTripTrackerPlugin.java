package com.maggotkingtriptracker;

import com.google.inject.Provides;
import com.maggotkingtriptracker.diagnostic.DiagnosticRecorder;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private MaggotKingTripTrackerConfig config;

	private DiagnosticRecorder diagnosticRecorder;

	@Override
	protected void startUp() throws Exception
	{
		diagnosticRecorder = new DiagnosticRecorder(client, this::getPluginDirectory);
		eventBus.register(diagnosticRecorder);
		boolean diagnosticMode = config.diagnosticMode();
		clientThread.invokeLater(() -> diagnosticRecorder.setEnabled(diagnosticMode));
		log.debug("Maggot King Trip Tracker started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(diagnosticRecorder);
		diagnosticRecorder.shutDown();
		diagnosticRecorder = null;
		log.debug("Maggot King Trip Tracker stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MaggotKingTripTrackerConfig.GROUP.equals(event.getGroup()) || !"diagnosticMode".equals(event.getKey()))
		{
			return;
		}

		boolean diagnosticMode = config.diagnosticMode();
		DiagnosticRecorder recorder = diagnosticRecorder;
		clientThread.invokeLater(() -> recorder.setEnabled(diagnosticMode));
	}

	@Provides
	MaggotKingTripTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MaggotKingTripTrackerConfig.class);
	}
}
