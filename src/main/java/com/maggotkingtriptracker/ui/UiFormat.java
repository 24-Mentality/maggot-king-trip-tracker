package com.maggotkingtriptracker.ui;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

final class UiFormat
{
	static final Color PROFIT = ColorScheme.PROGRESS_COMPLETE_COLOR;
	static final Color LOSS = ColorScheme.PROGRESS_ERROR_COLOR;
	static final Color UNIQUE_BORDER = new Color(255, 196, 0);
	static final Color MUTED_TEXT = ColorScheme.LIGHT_GRAY_COLOR.darker();

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm");

	private UiFormat()
	{
	}

	/**
	 * Short gp value such as "1.25M" or "-340K".
	 */
	static String gp(long value)
	{
		String formatted = QuantityFormatter.quantityToStackSize(Math.abs(value));
		return value < 0 ? "-" + formatted : formatted;
	}

	static String fullGp(long value)
	{
		return QuantityFormatter.formatNumber(value) + " gp";
	}

	static Color profitColor(long value)
	{
		return value < 0 ? LOSS : PROFIT;
	}

	static String duration(long ms)
	{
		long totalSeconds = Math.max(0, ms / 1000);
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		return hours > 0
			? String.format("%d:%02d:%02d", hours, minutes, seconds)
			: String.format("%d:%02d", minutes, seconds);
	}

	static String killTime(Long ms)
	{
		if (ms == null)
		{
			return "-";
		}
		long tenths = Math.round(ms / 100.0);
		return String.format("%d:%02d.%d", tenths / 600, (tenths / 10) % 60, tenths % 10);
	}

	static String dateTime(long epochMs)
	{
		return DATE_TIME.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/**
	 * "Label: value" in the style of RuneLite's XP and loot trackers: grey label, white value.
	 */
	static String pair(String label, String value)
	{
		return "<html><font color='#a5a5a5'>" + html(label) + ":</font> <font color='#ffffff'>" + html(value) + "</font></html>";
	}

	static String html(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
