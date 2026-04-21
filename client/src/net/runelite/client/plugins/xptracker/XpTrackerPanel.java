package net.runelite.client.plugins.xptracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.inject.Inject;
import net.runelite.api.Experience;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

class XpTrackerPanel extends PluginPanel
{
	private static final Dimension XP_BAR_SIZE = new Dimension(100, 20);

	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
	private final JLabel summary = new JLabel("No XP gained yet", SwingConstants.CENTER);
	private final SkillIconManager skillIconManager;
	private final XpTrackerPlugin plugin;

	@Inject
	XpTrackerPanel(SkillIconManager skillIconManager, XpTrackerPlugin plugin)
	{
		this.skillIconManager = skillIconManager;
		this.plugin = plugin;
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("XP Tracker");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		summary.setForeground(Color.LIGHT_GRAY);
		summary.setFont(FontManager.getRunescapeSmallFont());
		add(summary);

		JButton reset = new JButton("Reset");
		reset.setFocusable(false);
		reset.addActionListener(event -> plugin.reset());
		add(reset);

		list.setOpaque(false);
		add(list);
	}

	void rebuild(Collection<XpTrackerPlugin.XpState> skills, int totalGained, int xpPerHour)
	{
		list.removeAll();
		summary.setText(String.format("%,d XP gained - %,d xp / hr", totalGained, xpPerHour));

		for (XpTrackerPlugin.XpState state : skills)
		{
			list.add(row(state));
		}

		revalidate();
		repaint();
	}

	private JPanel row(XpTrackerPlugin.XpState state)
	{
		JPanel row = new JPanel(new BorderLayout(0, 3));
		row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 86));

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);

		JLabel skillIcon = new JLabel();
		skillIcon.setPreferredSize(new Dimension(20, 0));
		if (skillIconManager != null)
		{
			skillIcon.setIcon(new ImageIcon(skillIconManager.getSkillImage(state.getSkill(), true)));
		}

		JPanel title = new JPanel();
		title.setOpaque(false);
		JLabel skillName = new JLabel(state.getSkill().getName());
		skillName.setForeground(Color.WHITE);
		skillName.setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
		JLabel xpGained = new JLabel(" (XP Gained: " + String.format("%,d", state.getGained()) + ")");
		xpGained.setForeground(Color.WHITE);
		xpGained.setFont(FontManager.getRunescapeSmallFont().deriveFont(17f));
		title.add(skillName);
		title.add(xpGained);

		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(20, 0));

		header.add(skillIcon, BorderLayout.WEST);
		header.add(title, BorderLayout.CENTER);
		header.add(spacer, BorderLayout.EAST);

		ProgressBar bar = new ProgressBar();
		bar.setBackground(new Color(38, 58, 70));
		bar.setForeground(new Color(70, 170, 210));
		bar.setPreferredSize(XP_BAR_SIZE);
		configureProgress(bar, state);

		row.add(header, BorderLayout.NORTH);
		row.add(barWrapper(bar), BorderLayout.CENTER);
		row.add(details(state), BorderLayout.SOUTH);
		return row;
	}

	private static JPanel barWrapper(ProgressBar bar)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.add(bar, BorderLayout.SOUTH);
		return wrapper;
	}

	private static void configureProgress(ProgressBar bar, XpTrackerPlugin.XpState state)
	{
		int currentLevel = Math.max(1, Experience.getLevelForXp(Math.max(0, state.getCurrentXp())));
		int nextLevel = Math.min(Experience.MAX_REAL_LEVEL, currentLevel + 1);
		int levelStart = Experience.getXpForLevel(currentLevel);
		int levelEnd = nextLevel > currentLevel ? Experience.getXpForLevel(nextLevel) : Experience.getXpForLevel(currentLevel);
		int intoLevel = Math.max(0, state.getCurrentXp() - levelStart);
		int needed = Math.max(1, levelEnd - levelStart);

		bar.setMaximumValue(needed);
		bar.setCost(Math.min(needed, intoLevel));
		bar.setLeftLabel(String.valueOf(currentLevel));
		bar.setRightLabel(nextLevel > currentLevel ? String.valueOf(nextLevel) : "99");
		bar.setCenterLabel(String.format("%,d / %,d", intoLevel, needed));
	}

	private static JPanel details(XpTrackerPlugin.XpState state)
	{
		int xpHr = XpTrackerPlugin.xpPerHour(state);
		int remaining = xpRemaining(state);
		String ttl = xpHr <= 0 || remaining <= 0 ? "--:--" : formatDuration((long) remaining * 3600000L / xpHr);
		int actionsLeft = state.getXpPerAction() <= 0 ? 0 : (int) Math.ceil((double) remaining / state.getXpPerAction());

		JPanel details = new JPanel(new GridLayout(2, 1, 0, 0));
		details.setOpaque(false);
		details.add(detailLine(String.format("%,d XP / hr | %s TTL", xpHr, ttl)));
		details.add(detailLine(String.format("%,d XP left | %,d actions", remaining, actionsLeft)));
		return details;
	}

	private static JLabel detailLine(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setForeground(Color.LIGHT_GRAY);
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	private static int xpRemaining(XpTrackerPlugin.XpState state)
	{
		int level = Math.max(1, Experience.getLevelForXp(Math.max(0, state.getCurrentXp())));
		if (level >= Experience.MAX_REAL_LEVEL)
		{
			return 0;
		}
		return Math.max(0, Experience.getXpForLevel(level + 1) - state.getCurrentXp());
	}

	private static String formatDuration(long millis)
	{
		long totalSeconds = Math.max(0L, millis / 1000L);
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 99)
		{
			return "99h+";
		}
		return hours > 0 ? String.format("%dh %02dm %02ds", hours, minutes, seconds) : String.format("%dm %02ds", minutes, seconds);
	}
}
