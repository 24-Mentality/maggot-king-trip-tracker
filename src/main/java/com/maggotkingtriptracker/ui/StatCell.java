package com.maggotkingtriptracker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A small caption above a large number.
 */
class StatCell extends JPanel
{
	private final JLabel value = new JLabel();

	StatCell(String caption, boolean large)
	{
		super(new BorderLayout());
		setOpaque(false);

		JLabel captionLabel = new JLabel(caption);
		captionLabel.setFont(FontManager.getRunescapeSmallFont());
		captionLabel.setForeground(UiFormat.MUTED_TEXT);
		add(captionLabel, BorderLayout.NORTH);

		value.setFont(large ? FontManager.getRunescapeBoldFont().deriveFont(20f) : FontManager.getRunescapeBoldFont());
		value.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(value, BorderLayout.CENTER);
	}

	void setValue(String text)
	{
		value.setText(text);
	}

	void setValue(String text, Color color, String tooltip)
	{
		value.setText(text);
		value.setForeground(color);
		setToolTipText(tooltip);
		value.setToolTipText(tooltip);
	}
}
