package net.runelite.client.plugins.qol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class QolPatterns
{
	private QolPatterns()
	{
	}

	public static List<String> split(String input)
	{
		if (input == null || input.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		List<String> values = new ArrayList<>();
		for (String token : input.split("[,;\\n]"))
		{
			String value = token.trim().toLowerCase(Locale.ENGLISH);
			if (!value.isEmpty())
			{
				values.add(value);
			}
		}
		return values;
	}

	public static boolean matches(String value, String patterns)
	{
		return matches(value, split(patterns));
	}

	public static boolean matches(String value, List<String> patterns)
	{
		if (value == null || patterns == null || patterns.isEmpty())
		{
			return false;
		}

		String lower = value.toLowerCase(Locale.ENGLISH);
		for (String pattern : patterns)
		{
			if (pattern.startsWith("#"))
			{
				continue;
			}
			if (pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0)
			{
				String regex = Pattern.quote(pattern)
					.replace("\\Q*\\E", ".*")
					.replace("\\Q?\\E", ".");
				if (lower.matches(regex))
				{
					return true;
				}
			}
			else if (lower.equals(pattern) || lower.contains(pattern))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean matchesId(int id, String patterns)
	{
		for (String token : split(patterns))
		{
			try
			{
				if (Integer.parseInt(token) == id)
				{
					return true;
				}
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		return false;
	}
}
