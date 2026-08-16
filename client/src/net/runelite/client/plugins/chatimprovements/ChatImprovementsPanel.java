package net.runelite.client.plugins.chatimprovements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class ChatImprovementsPanel extends PluginPanel
{
	private final JTextField filter = new JTextField();
	private final JTextPane history = new JTextPane();
	private Runnable filterListener;

	ChatImprovementsPanel()
	{
		super(false);
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		filter.setToolTipText("Filter persistent chat history");
		filter.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { changed(); }
			@Override public void removeUpdate(DocumentEvent e) { changed(); }
			@Override public void changedUpdate(DocumentEvent e) { changed(); }
			private void changed() { if (filterListener != null) filterListener.run(); }
		});
		history.setEditable(false);
		history.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		history.setForeground(ColorScheme.TEXT_COLOR);
		JScrollPane scroll = new JScrollPane(history);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		add(filter, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
	}

	void setFilterListener(Runnable listener) { filterListener = listener; }
	String getFilterText() { return filter.getText(); }

	void rebuild(Collection<ChatImprovementsPlugin.ChatRecord> records, boolean timestamps, Color highlightColor)
	{
		StyledDocument document = history.getStyledDocument();
		try
		{
			document.remove(0, document.getLength());
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			for (ChatImprovementsPlugin.ChatRecord record : records)
			{
				if (timestamps)
				{
					append(document, "[" + format.format(new Date(record.getTimestamp())) + "] ", Color.GRAY, false);
				}
				if (!record.getName().isEmpty())
				{
					append(document, record.getName() + ": ", ColorScheme.BRAND_ORANGE, true);
				}
				append(document, record.getMessage() + "\n", record.isHighlighted() ? highlightColor : ColorScheme.TEXT_COLOR, record.isHighlighted());
			}
			history.setCaretPosition(document.getLength());
		}
		catch (BadLocationException ignored)
		{
		}
	}

	private static void append(StyledDocument document, String text, Color color, boolean bold) throws BadLocationException
	{
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		StyleConstants.setBold(style, bold);
		document.insertString(document.getLength(), text, style);
	}
}
