package com.maggotkingtriptracker.ui;

import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A titled card of text lines, used for the dryness, egg and polish summaries.
 */
class InfoCard extends JPanel
{
	private final JPanel lines = new JPanel();

	InfoCard(String title)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		setAlignmentX(LEFT_ALIGNMENT);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		titleLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(titleLabel);

		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.setOpaque(false);
		lines.setAlignmentX(LEFT_ALIGNMENT);
		lines.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		add(lines);
	}

	/**
	 * @param rows HTML-free text; each becomes one wrapped line
	 */
	void setLines(List<String> rows)
	{
		lines.removeAll();
		for (String row : rows)
		{
			JLabel label = new JLabel("<html>" + UiFormat.html(row) + "</html>");
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(UiFormat.MUTED_TEXT.brighter());
			label.setAlignmentX(LEFT_ALIGNMENT);
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			lines.add(label);
		}
		revalidate();
		repaint();
	}
}
