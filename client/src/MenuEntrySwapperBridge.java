import java.util.Locale;
import net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperState;

final class MenuEntrySwapperBridge
{
	private MenuEntrySwapperBridge()
	{
	}

	static void apply(Class262 entries)
	{
		if (!MenuEntrySwapperState.enabled || entries == null || MenuEntrySwapperState.preferredOptions == null)
		{
			return;
		}

		for (String rawRule : MenuEntrySwapperState.preferredOptions.split("[,;\\n]"))
		{
			String rule = rawRule.trim();
			if (rule.isEmpty())
			{
				continue;
			}
			String optionRule = rule;
			String targetRule = null;
			int separator = rule.indexOf('@');
			if (separator >= 0)
			{
				optionRule = rule.substring(0, separator).trim();
				targetRule = rule.substring(separator + 1).trim();
			}

			Class348_Sub42_Sub12 match = find(entries, optionRule, targetRule);
			if (match != null)
			{
				entries.method1999(match, -20180);
				return;
			}
		}
	}

	private static Class348_Sub42_Sub12 find(Class262 entries, String optionRule, String targetRule)
	{
		String optionNeedle = optionRule.toLowerCase(Locale.ENGLISH);
		String targetNeedle = targetRule == null ? null : targetRule.toLowerCase(Locale.ENGLISH);
		for (Class348_Sub42_Sub12 entry = (Class348_Sub42_Sub12) entries.method1995(4);
			entry != null;
			entry = (Class348_Sub42_Sub12) entries.method1990((byte) 83))
		{
			String option = clean(entry.aString9593);
			String target = clean(entry.aString9601);
			boolean optionMatches = MenuEntrySwapperState.exactMatch ? option.equals(optionNeedle) : option.contains(optionNeedle);
			boolean targetMatches = targetNeedle == null || target.contains(targetNeedle);
			if (optionMatches && targetMatches && !"cancel".equals(option))
			{
				return entry;
			}
		}
		return null;
	}

	private static String clean(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replaceAll("<[^>]+>", "").trim().toLowerCase(Locale.ENGLISH);
	}
}
