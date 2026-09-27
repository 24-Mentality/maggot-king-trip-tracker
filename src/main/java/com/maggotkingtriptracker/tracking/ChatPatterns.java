package com.maggotkingtriptracker.tracking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * Parsers for the game messages the tracker relies on. Input is the raw chat message, including color tags.
 */
public final class ChatPatterns
{
	private static final Pattern KILL_COUNT = Pattern.compile("^Your (.+) kill count is: ([\\d,]+)\\.?$");
	private static final Pattern FIGHT_DURATION = Pattern.compile("^Fight duration: (?:(\\d+):)?(\\d+):(\\d+(?:\\.\\d+)?)");

	public static final String DEATH = "Oh dear, you are dead!";
	public static final String PET_FOLLOWER = "You have a funny feeling like you're being followed.";
	public static final String PET_BACKPACK = "You feel something weird sneaking into your backpack.";
	public static final String PET_DUPLICATE = "You have a funny feeling like you would have been followed";

	private ChatPatterns()
	{
	}

	/**
	 * @return the kill count if this is the kill-count message for the given boss, otherwise null
	 */
	public static Integer killCount(String message, String bossName)
	{
		Matcher m = KILL_COUNT.matcher(Text.removeTags(message));
		if (!m.matches() || !m.group(1).equalsIgnoreCase(bossName))
		{
			return null;
		}
		return Integer.parseInt(m.group(2).replace(",", ""));
	}

	/**
	 * @return the fight duration in milliseconds from a "Fight duration: m:ss.cc" message, otherwise null
	 */
	public static Long fightDurationMs(String message)
	{
		Matcher m = FIGHT_DURATION.matcher(Text.removeTags(message));
		if (!m.find())
		{
			return null;
		}
		long hours = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
		long minutes = Long.parseLong(m.group(2));
		double seconds = Double.parseDouble(m.group(3));
		return hours * 3_600_000 + minutes * 60_000 + Math.round(seconds * 1000);
	}

	public static boolean isPetMessage(String message)
	{
		String text = Text.removeTags(message);
		return text.equals(PET_FOLLOWER) || text.equals(PET_BACKPACK) || text.startsWith(PET_DUPLICATE);
	}

	public static boolean isDeathMessage(String message)
	{
		return Text.removeTags(message).equals(DEATH);
	}
}
