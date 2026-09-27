package com.maggotkingtriptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(MaggotKingTripTrackerConfig.GROUP)
public interface MaggotKingTripTrackerConfig extends Config
{
	String GROUP = "maggotkingtriptracker";

	@ConfigSection(
		name = "Trips",
		description = "When trips start and end",
		position = 0
	)
	String tripsSection = "trips";

	@ConfigSection(
		name = "Charges",
		description = "Charged item costs",
		position = 1
	)
	String chargesSection = "charges";

	@ConfigSection(
		name = "Loot alerts",
		description = "Notifications for good drops",
		position = 2
	)
	String alertsSection = "alerts";

	@ConfigSection(
		name = "Display",
		description = "Side panel options",
		position = 3
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Developer",
		description = "Settings used to collect data for plugin development",
		position = 100,
		closedByDefault = true
	)
	String developerSection = "developer";

	@ConfigItem(
		keyName = "logoutGraceMinutes",
		name = "Logout grace period",
		description = "After logging out in the lair, the trip continues if you are back in the lair within this time",
		section = tripsSection,
		position = 0
	)
	@Range(max = 60)
	@Units(Units.MINUTES)
	default int logoutGraceMinutes()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "mergeReentries",
		name = "Merge re-entries",
		description = "Count leaving and re-entering the lair within the merge window as one trip",
		section = tripsSection,
		position = 1
	)
	default boolean mergeReentries()
	{
		return false;
	}

	@ConfigItem(
		keyName = "mergeWindowMinutes",
		name = "Merge window",
		description = "How soon you must re-enter the lair for it to count as the same trip",
		section = tripsSection,
		position = 2
	)
	@Range(min = 1, max = 60)
	@Units(Units.MINUTES)
	default int mergeWindowMinutes()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "countPreEntrySupplies",
		name = "Count supplies used before entry",
		description = "Add food, potions and spells used in the 60 seconds before entering the lair to the trip",
		section = tripsSection,
		position = 3
	)
	default boolean countPreEntrySupplies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tomePage",
		name = "Tome of fire pages",
		description = "Which page's GE price is used for Tome of fire charges (20 charges per page)",
		section = chargesSection,
		position = 0
	)
	default TomePage tomePage()
	{
		return TomePage.SEARING;
	}

	@ConfigItem(
		keyName = "alertUniques",
		name = "Unique drops",
		description = "Notify when you get an Elder venator fang or Crimson kisten",
		section = alertsSection,
		position = 0
	)
	default boolean alertUniques()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertPet",
		name = "Pet",
		description = "Notify when you get the Maggot King pet, from a kill or an egg",
		section = alertsSection,
		position = 1
	)
	default boolean alertPet()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertValue",
		name = "Drops worth at least",
		description = "Notify for any Maggot King drop (including polished tarnished items) worth at least this"
			+ " many gp. 0 turns it off.",
		section = alertsSection,
		position = 2
	)
	default int alertValue()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "alertNotification",
		name = "Notification",
		description = "How loot alerts are shown",
		section = alertsSection,
		position = 3
	)
	default Notification alertNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "openPanelOnEntry",
		name = "Open panel in the lair",
		description = "Open this plugin's side panel on the Trip tab when you enter the Maggot King's lair",
		section = displaySection,
		position = 0
	)
	default boolean openPanelOnEntry()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCurrentValue",
		name = "Show today's value",
		description = "On the Lifetime tab, also show all loot valued at today's GE prices",
		section = displaySection,
		position = 1
	)
	default boolean showCurrentValue()
	{
		return false;
	}

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
