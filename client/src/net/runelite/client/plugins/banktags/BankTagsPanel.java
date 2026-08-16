package net.runelite.client.plugins.banktags;

import com.GameClient;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class BankTagsPanel extends PluginPanel
{
	private final JTextField search = new JTextField();
	private final JLabel summary = new JLabel("Open a bank to populate items", SwingConstants.CENTER);
	private final JPanel groups = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
	private Runnable searchListener;

	BankTagsPanel()
	{
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JLabel title = new JLabel("Bank Tags");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		add(title);
		search.setToolTipText("Search current bank items");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { changed(); }
			@Override public void removeUpdate(DocumentEvent e) { changed(); }
			@Override public void changedUpdate(DocumentEvent e) { changed(); }
			private void changed() { if (searchListener != null) searchListener.run(); }
		});
		add(search);
		summary.setForeground(Color.LIGHT_GRAY);
		summary.setFont(FontManager.getRunescapeSmallFont());
		add(summary);
		groups.setOpaque(false);
		add(groups);
	}

	void setSearchListener(Runnable listener) { this.searchListener = listener; }
	String getSearchText() { return search.getText(); }

	void rebuild(Map<String, List<GameClient.ItemStackInfo>> data, int itemCount, int totalValue, boolean showValues)
	{
		groups.removeAll();
		summary.setText(itemCount == 0 ? "Open a bank to populate items" : String.format("%,d slots - %,d gp", itemCount, totalValue));
		for (Map.Entry<String, List<GameClient.ItemStackInfo>> entry : data.entrySet())
		{
			if (entry.getValue().isEmpty())
			{
				continue;
			}
			JPanel group = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
			group.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			group.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			JLabel heading = new JLabel(entry.getKey() + " (" + entry.getValue().size() + ")");
			heading.setForeground(ColorScheme.BRAND_ORANGE);
			heading.setFont(FontManager.getRunescapeSmallFont().deriveFont(java.awt.Font.BOLD));
			group.add(heading);
			for (GameClient.ItemStackInfo item : entry.getValue())
			{
				JPanel row = new JPanel(new BorderLayout(6, 0));
				row.setOpaque(false);
				row.setPreferredSize(new Dimension(PANEL_WIDTH - 40, 20));
				JLabel name = new JLabel(item.getName());
				name.setForeground(Color.WHITE);
				name.setFont(FontManager.getRunescapeSmallFont());
				String right = String.format("%,d", item.getQuantity());
				if (showValues && item.getStackValue() > 0)
				{
					right += " / " + String.format("%,d", item.getStackValue());
				}
				JLabel amount = new JLabel(right, SwingConstants.RIGHT);
				amount.setForeground(Color.LIGHT_GRAY);
				amount.setFont(FontManager.getRunescapeSmallFont());
				row.add(name, BorderLayout.CENTER);
				row.add(amount, BorderLayout.EAST);
				group.add(row);
			}
			groups.add(group);
		}
		groups.revalidate();
		groups.repaint();
	}
}
