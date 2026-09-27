package com.maggotkingtriptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(MaggotKingTripTrackerConfig.GROUP)
public interface MaggotKingTripTrackerConfig extends Config
{
	String GROUP = "maggotkingtriptracker";

	@ConfigSection(
		name = "Developer",
		description = "Settings used to collect data for plugin development",
		position = 100,
		closedByDefault = true
	)
	String developerSection = "developer";

	@ConfigItem(
		keyName = "diagnosticMode",
		name = "Diagnostic mode",
		description = "Append Maggot King related game events (chat, clicks, inventory changes, loot) to diagnostic.log"
			+ " in this plugin's data folder. Leave off unless you are collecting data for development.",
		section = developerSection,
		position = 0
	)
	default boolean diagnosticMode()
	{
		return false;
	}
}
