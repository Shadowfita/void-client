package net.runelite.client.plugins.qol;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class QolIcon
{
	private QolIcon()
	{
	}

	public static BufferedImage letter(String text, Color color)
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(0, 0, 0, 120));
			graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
			graphics.setColor(color);
			graphics.drawRoundRect(1, 1, 13, 13, 4, 4);
			graphics.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
			FontMetrics metrics = graphics.getFontMetrics();
			String label = text == null || text.isEmpty() ? "?" : text.substring(0, 1).toUpperCase();
			graphics.drawString(label, (16 - metrics.stringWidth(label)) / 2, 12);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
