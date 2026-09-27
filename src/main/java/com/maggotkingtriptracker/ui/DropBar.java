package com.maggotkingtriptracker.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;

/**
 * A bar in the drop chances card. Expected mode fills left to right with a red-to-green colour and a percentage;
 * received mode grows from the centre, green to the right when ahead of expectation and red to the left when behind.
 */
class DropBar extends JComponent
{
	private static final Color BACKGROUND = new Color(30, 30, 30);
	private static final Color BORDER = new Color(165, 165, 165);
	private static final Color CENTRE_LINE = new Color(192, 192, 192);
	private static final Color AHEAD = new Color(0, 100, 0);
	private static final Color BEHIND = new Color(150, 3, 0);

	private boolean received;
	private double fraction;
	private double difference;
	private double scale = 1;

	DropBar()
	{
		setPreferredSize(new Dimension(0, 20));
		setFont(FontManager.getRunescapeSmallFont());
	}

	void showExpected(double fraction)
	{
		this.received = false;
		this.fraction = Math.max(0, Math.min(1, fraction));
		repaint();
	}

	/**
	 * @param scale the largest difference among the rows (at least 1), which fills half the bar
	 */
	void showReceived(double difference, double scale)
	{
		this.received = true;
		this.difference = difference;
		this.scale = Math.max(1, scale);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int width = getWidth();
		int height = getHeight();
		g2.setColor(BACKGROUND);
		g2.fillRect(0, 0, width, height);

		int innerX = 1;
		int innerWidth = width - 2;
		FontMetrics metrics = g2.getFontMetrics(getFont());
		int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();

		if (!received)
		{
			g2.setColor(expectedColor(fraction));
			g2.fillRect(innerX, 1, (int) Math.round(innerWidth * fraction), height - 2);
			String text = String.format(Locale.ROOT, "%.1f%%", fraction * 100);
			g2.setColor(Color.WHITE);
			g2.drawString(text, (width - metrics.stringWidth(text)) / 2, textY);
		}
		else
		{
			int centre = width / 2;
			int half = innerWidth / 2;
			int length = (int) Math.round(half * Math.min(1, Math.abs(difference) / scale));
			String text = String.format(Locale.ROOT, "%+.2f", difference);
			g2.setColor(difference >= 0 ? AHEAD : BEHIND);
			if (difference >= 0)
			{
				g2.fillRect(centre, 1, length, height - 2);
			}
			else
			{
				g2.fillRect(centre - length, 1, length, height - 2);
			}
			g2.setColor(CENTRE_LINE);
			g2.drawLine(centre, 1, centre, height - 2);

			// The number sits on the empty side of the centre line
			g2.setColor(Color.WHITE);
			int textWidth = metrics.stringWidth(text);
			int textX = difference >= 0 ? centre - 4 - textWidth : centre + 4;
			g2.drawString(text, textX, textY);
		}

		g2.setColor(BORDER);
		g2.drawRect(0, 0, width - 1, height - 1);
		g2.dispose();
	}

	/**
	 * Red at 0%, amber at 50%, green at 100%.
	 */
	static Color expectedColor(double fraction)
	{
		int red = (int) Math.round(Math.min(100, 200 * (1 - fraction)));
		int green = (int) Math.round(Math.min(100, 200 * fraction));
		return new Color(red, green, 0);
	}
}
