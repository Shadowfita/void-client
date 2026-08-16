package net.runelite.client.plugins.cluehelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class ClueHelperPanel extends PluginPanel
{
	private final JTextArea clueText = new JTextArea(5, 20);
	private final JTextArea solutionText = new JTextArea();
	private final JLabel status = new JLabel("Paste or type a Treasure Trail clue.", SwingConstants.LEFT);
	private Runnable lookupListener;

	ClueHelperPanel()
	{
		super(false);
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		clueText.setLineWrap(true);
		clueText.setWrapStyleWord(true);
		clueText.setToolTipText("Enter the clue text exactly or approximately");
		JScrollPane inputScroll = new JScrollPane(clueText);
		inputScroll.setPreferredSize(new Dimension(0, 105));

		JButton lookup = new JButton("Look up clue");
		lookup.addActionListener(event ->
		{
			if (lookupListener != null)
			{
				lookupListener.run();
			}
		});
		JButton clear = new JButton("Clear");
		clear.addActionListener(event ->
		{
			clueText.setText("");
			solutionText.setText("");
			status.setText("Paste or type a Treasure Trail clue.");
		});
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttons.setOpaque(false);
		buttons.add(lookup);
		buttons.add(clear);

		JPanel input = new JPanel(new BorderLayout(0, 6));
		input.setOpaque(false);
		input.add(inputScroll, BorderLayout.CENTER);
		input.add(buttons, BorderLayout.SOUTH);

		solutionText.setEditable(false);
		solutionText.setLineWrap(true);
		solutionText.setWrapStyleWord(true);
		solutionText.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		solutionText.setForeground(ColorScheme.TEXT_COLOR);
		solutionText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane outputScroll = new JScrollPane(solutionText);
		outputScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR));

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(input, BorderLayout.NORTH);
		add(outputScroll, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);
	}

	void setLookupListener(Runnable listener)
	{
		lookupListener = listener;
	}

	String getClueText()
	{
		return clueText.getText();
	}

	void setClueText(String text)
	{
		clueText.setText(text == null ? "" : text);
		clueText.setCaretPosition(0);
	}

	void showSolution(ClueSolution solution)
	{
		solutionText.setForeground(ColorScheme.TEXT_COLOR);
		solutionText.setText(solution.format());
		solutionText.setCaretPosition(0);
		status.setText("Recognised " + solution.getType().toLowerCase() + " clue.");
	}

	void showNoMatch(List<String> suggestions)
	{
		StringBuilder text = new StringBuilder("This clue is not in the bundled 2011 reference set.");
		if (suggestions != null && !suggestions.isEmpty())
		{
			text.append("\n\nPossible matching clue text:\n");
			for (String suggestion : suggestions)
			{
				text.append("\n• ").append(suggestion);
			}
		}
		text.append("\n\nYou can still keep the clue here while solving it manually. The database deliberately excludes OSRS-only clues.");
		solutionText.setForeground(new Color(220, 170, 100));
		solutionText.setText(text.toString());
		solutionText.setCaretPosition(0);
		status.setText("No exact 634-era match found.");
	}
}
