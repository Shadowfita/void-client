package net.runelite.client.plugins.cluehelper;

import lombok.Value;

@Value
class ClueSolution
{
	String type;
	String title;
	String location;
	String answer;
	String notes;

	String format()
	{
		StringBuilder out = new StringBuilder();
		append(out, "Type", type);
		append(out, "Target", title);
		append(out, "Location", location);
		append(out, "Answer", answer);
		append(out, "Notes", notes);
		return out.toString().trim();
	}

	private static void append(StringBuilder out, String label, String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return;
		}
		if (out.length() > 0)
		{
			out.append("\n\n");
		}
		out.append(label).append(":\n").append(value.trim());
	}
}
