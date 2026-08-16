package net.runelite.client.plugins.cluehelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Revision-appropriate Treasure Trails reference data. This deliberately omits
 * OSRS-only locations and NPCs introduced after the 634 era.
 */
final class ClueDatabase
{
	private static final Map<String, ClueSolution> EXACT = new HashMap<String, ClueSolution>();
	private static final List<AnagramTarget> ANAGRAM_TARGETS = new ArrayList<AnagramTarget>();

	static
	{
		// Classic anagrams and challenge-scroll answers available in the 2010/2011 game.
		anagram("A BAKER", "Baraek", "Varrock square", "5", "Speak to Baraek, the fur trader.");
		anagram("ARE COL", "Oracle", "Ice Mountain", "48", "The Oracle is on the northern slope of Ice Mountain.");
		anagram("A BAS", "Saba", "Death Plateau", null, "Speak to Saba in the cave southeast of Death Plateau.");
		anagram("BAIL TRIMS", "Brimstail", "Tree Gnome Stronghold", null, "Brimstail is in the cave southwest of the Stronghold bank.");
		anagram("BLUE GRIM GUIDED", "Lumbridge Guide", "Lumbridge", null, "Speak to the Lumbridge Guide near the castle.");
		anagram("BY LOOK", "Bolkoy", "Tree Gnome Village", "13", "Bolkoy is in the village general store.");
		anagram("C ON GAME HOC", "Gnome Coach", "Gnome Ball field", "6", "Speak to the coach at the Gnome Ball field.");
		anagram("DEKAGRAM", "Dark Mage", "Centre of the Abyss", "13", "Enter the Abyss and speak to the Dark Mage.");
		anagram("DO SAY MORE", "Doomsayer", "East Lumbridge", "95", "Speak to the Doomsayer east of Lumbridge Castle.");
		anagram("DRAGONS LAMENT", "Strange Old Man", "Barrows", "40", "The Strange Old Man stands near the Barrows mounds.");
		anagram("DT RUN B", "Brundt the Chieftain", "Rellekka longhall", "5", "Speak to Brundt inside the Rellekka longhall.");
		anagram("EEK ZERO OP", "Zoo keeper", "Ardougne Zoo", "50", "The exact animal count can vary with quest state.");
		anagram("EL OW", "Lowe", "Varrock archery shop", null, "Speak to Lowe in the archery shop east of Varrock square.");
		anagram("GOBLIN KERN", "King Bolren", "Tree Gnome Village", null, "Speak to King Bolren in the centre of the village.");
		anagram("GOT A BOY", "Gabooty", "Tai Bwo Wannai", "11", "Speak to Gabooty near the village centre.");
		anagram("HALT US", "Luthas", "Karamja banana plantation", null, "Speak to Luthas in the plantation house.");
		anagram("HEORIC", "Eohric", "Burthorpe Castle, top floor", "36", "Eohric is upstairs in Burthorpe Castle.");
		anagram("ICY FE", "Fycie", "Feldip Hills", null, "Speak to Fycie near Rantz in the eastern Feldip Hills.");
		anagram("KAY SIR", "Sir Kay", "Camelot Castle courtyard", "6", "Speak to Sir Kay in the courtyard.");
		anagram("LEAKEY", "Kaylee", "Rising Sun Inn, Falador", "18", "Speak to Kaylee in the Rising Sun Inn.");
		anagram("LARK IN DOG", "King Roald", "Varrock Palace", "24", "Speak to King Roald on the ground floor.");
		anagram("ME IF", "Femi", "Tree Gnome Stronghold gate", null, "Speak to Femi outside the Stronghold entrance.");
		anagram("MOTHERBOARD", "Brother Omad", "Monastery south of Ardougne", "129", "Speak to Brother Omad inside the monastery.");
		anagram("NOD MED", "Edmond", "North-west East Ardougne", "3", "Edmond is behind his house near the city wall.");
		anagram("O BIRDZ A ZANY EN PC", "Cap'n Izzy No-Beard", "Brimhaven Agility Arena", "33", "Speak to Cap'n Izzy No-Beard outside the arena.");
		anagram("OK CO", "Cook", "Lumbridge Castle kitchen", "9", "Speak to the Cook on the ground floor.");
		anagram("PEATY PERT", "Party Pete", "Falador Party Room", null, "Speak to Party Pete in the Party Room.");
		anagram("QUE SIR", "Squire", "Falador Castle courtyard", "654", "Speak to the Squire in the courtyard.");
		anagram("R AK MI", "Karim", "Al Kharid kebab shop", "5", "Speak to Karim in the kebab shop.");
		anagram("RAT MAT WITHIN", "Martin Thwait", "Rogues' Den", "2", "Speak to Martin Thwait near the Rogues' Den bank.");
		anagram("RATAI", "Taria", "Rimmington bush patch", "7", "Speak to Taria by the bush patch.");
		anagram("SAND NUT", "Dunstan", "North-east Burthorpe", "8", "Speak to Dunstan beside the anvil.");
		anagram("SLIDE WOMAN", "Wise Old Man", "Draynor Village", "28", "Speak to the Wise Old Man in his house.");
		anagram("SNAKES SO I SAIL", "Lisse Isaakson", "Neitiznot", "2", "Speak to Lisse Isaakson on Neitiznot.");
		anagram("THICKNO", "Hickton", "Catherby fletching shop", "2", "Speak to Hickton in the fletching shop.");
		anagram("UNLEASH NIGHT MIST", "Sigli the Huntsman", "Rellekka", "302", "Speak to Sigli near the Rellekka entrance.");
		anagram("VEIL VEDA", "Evil Dave", "Doris's basement, Edgeville", "666", "Speak to Evil Dave in the basement.");
		anagram("WOO AN EGG KIWI", "Awowogei", "Ape Atoll throne room", "24", "Speak to King Awowogei.");

		// Classic ciphers.
		exact("BMJ UIF LFCBC TFMMFS", solution("Cipher", "Ali the Kebab seller", "Pollnivneach", "399", "A Caesar-shifted clue."));
		exact("GUHCHO", solution("Cipher", "Drezel", "Paterdomus", "7", "Speak to Drezel beneath Paterdomus."));
		exact("ZHLUG ROG PDQ", solution("Cipher", "Kalphite-lair old man", "Kalphite Lair entrance", "150", "Speak to the old man near the lair entrance."));
		exact("OVEXON", solution("Cipher", "Eluned", "Isafdar / outside Lletya", "53,000", "Speak to Eluned at her current roaming location."));
		exact("VTYR APCNTGLW", solution("Cipher", "King Percival", "Fisher Realm", "5", "Travel to the Fisher Realm and speak to King Percival."));
		exact("UZZU MUJHRKYYKJ", solution("Cipher", "Otto Godblessed", "Otto's Grotto", "3", "Speak to Otto beside Baxtorian Falls."));
		exact("XJABSE USBJCPSO", solution("Cipher", "Traiborn", "Wizards' Tower, first floor", "3150", "Speak to Traiborn upstairs."));
		exact("HCKTA IQFHCVJGT", solution("Cipher", "Fairy Godfather", "Zanaris throne room", "64", "Speak to the Fairy Godfather in Zanaris."));

		// Common classic riddles/search clues. Matching is normalised and partial-safe.
		exact("TALK TO THE BARTENDER OF THE RUSTY ANCHOR IN PORT SARIM", solution("Cryptic", "Bartender", "Rusty Anchor, Port Sarim", null, null));
		exact("THE KEEPER OF MELZARS SPARE SKELETON ANAR", solution("Cryptic", "Oziach", "Edgeville", null, "Speak to Oziach west of Edgeville bank."));
		exact("SPEAK TO ULIZIUS", solution("Cryptic", "Ulizius", "Mort Myre Swamp gate", null, null));
		exact("SEARCH FOR A CRATE IN A BUILDING IN HEMENSTER", solution("Search", "Crate", "House northwest of the Ranging Guild", null, null));
		exact("SEARCH THE BUCKET IN THE PORT SARIM JAIL", solution("Search", "Bucket", "Port Sarim jail", null, "Refuse Shantay's fine to be sent to jail."));
		exact("SEARCH THE CRATES IN A BANK IN VARROCK", solution("Search", "Crates", "West Varrock bank basement", null, null));
		exact("SEARCH A BOOKCASE IN THE WIZARDS TOWER", solution("Search", "Bookcase", "Wizards' Tower ground floor", null, null));
		exact("SPEAK TO SARAH AT FALADOR FARM", solution("Cryptic", "Sarah", "Falador farm", null, null));
		exact("SEARCH FOR A CRATE ON THE GROUND FLOOR OF A HOUSE IN SEERS VILLAGE", solution("Search", "Crate", "House south of the Seers' Village pub", null, null));
		exact("SPEAK TO SIR KAY IN CAMELOT CASTLE", solution("Cryptic", "Sir Kay", "Camelot Castle courtyard", null, null));
		exact("FIND A CRATE CLOSE TO THE MONKS THAT LIKE TO PAAARTY", solution("Search", "Crate", "Ardougne monastery, east side", null, null));
		exact("IDENTIFY THE BACK OF THIS OVER ACTING BROTHER HES A LONG WAY FROM HOME", solution("Cryptic", "Hamid", "Duel Arena altar", null, null));
		exact("A TOWN WITH A DIFFERENT SORT OF NIGHT LIFE IS YOUR DESTINATION SEARCH FOR SOME CRATES IN ONE OF THE HOUSES", solution("Search", "Crate", "Canifis clothes shop", null, null));
		exact("SEARCH THE CRATE NEAR THE SOUTHERN GENERAL STORE IN PORT KHAZARD", solution("Search", "Crate", "Port Khazard", null, null));
		exact("SPEAK TO THE BARTENDER OF THE BLUE MOON INN IN VARROCK", solution("Cryptic", "Bartender", "Blue Moon Inn, Varrock", null, null));
		exact("THIS AVIATOR IS AT THE PEAK OF HIS PROFESSION", solution("Cryptic", "Captain Bleemadge", "White Wolf Mountain summit", null, null));
		exact("SEARCH THE CRATES IN THE SHED JUST NORTH OF EAST ARDOUGNE", solution("Search", "Crates", "Shed north of East Ardougne bank", null, null));
		exact("SEARCH THE CRATE IN THE TOAD AND CHICKEN PUB", solution("Search", "Crate", "Toad and Chicken, Burthorpe", null, null));
		exact("SEARCH CHESTS FOUND IN THE UPSTAIRS OF SHOPS IN PORT SARIM", solution("Search", "Chest", "Upstairs in Wydin's Food Store", null, null));
	}

	private ClueDatabase()
	{
	}

	static ClueSolution lookup(String input)
	{
		String normal = normalise(input);
		if (normal.isEmpty())
		{
			return null;
		}

		ClueSolution direct = EXACT.get(normal);
		if (direct != null)
		{
			return direct;
		}

		for (Map.Entry<String, ClueSolution> entry : EXACT.entrySet())
		{
			String key = entry.getKey();
			if (normal.contains(key) || (normal.length() > 18 && key.contains(normal)))
			{
				return entry.getValue();
			}
		}

		String candidate = stripAnagramPrefix(normal);
		String signature = signature(candidate);
		for (AnagramTarget target : ANAGRAM_TARGETS)
		{
			if (target.signature.equals(signature))
			{
				return target.solution;
			}
		}

		return null;
	}

	static List<String> suggestions(String input, int limit)
	{
		String normal = normalise(input);
		if (normal.isEmpty())
		{
			return Collections.emptyList();
		}
		List<ScoredText> scored = new ArrayList<ScoredText>();
		for (String key : EXACT.keySet())
		{
			int score = similarity(normal, key);
			if (score > 0)
			{
				scored.add(new ScoredText(key, score));
			}
		}
		Collections.sort(scored, new Comparator<ScoredText>()
		{
			@Override
			public int compare(ScoredText left, ScoredText right)
			{
				return Integer.compare(right.score, left.score);
			}
		});
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < scored.size() && result.size() < limit; i++)
		{
			result.add(scored.get(i).text);
		}
		return result;
	}

	private static void anagram(String text, String target, String location, String answer, String notes)
	{
		ClueSolution solution = solution("Anagram", target, location, answer, notes);
		exact(text, solution);
		ANAGRAM_TARGETS.add(new AnagramTarget(signature(text), solution));
	}

	private static void exact(String text, ClueSolution solution)
	{
		EXACT.put(normalise(text), solution);
	}

	private static ClueSolution solution(String type, String target, String location, String answer, String notes)
	{
		return new ClueSolution(type, target, location, answer, notes);
	}

	private static String stripAnagramPrefix(String text)
	{
		String[] prefixes = {
			"THIS ANAGRAM REVEALS WHO TO SPEAK TO NEXT ",
			"THE ANAGRAM REVEALS WHO TO SPEAK TO NEXT ",
			"ANAGRAM "
		};
		for (String prefix : prefixes)
		{
			if (text.startsWith(prefix))
			{
				return text.substring(prefix.length()).trim();
			}
		}
		return text;
	}

	private static String normalise(String value)
	{
		if (value == null)
		{
			return "";
		}
		String text = Normalizer.normalize(value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}+", "")
			.toUpperCase(Locale.ENGLISH)
			.replace('&', ' ')
			.replaceAll("<[^>]*>", " ")
			.replaceAll("[^A-Z0-9]+", " ")
			.trim()
			.replaceAll("\\s+", " ");
		return text;
	}

	private static String signature(String value)
	{
		char[] chars = normalise(value).replace(" ", "").toCharArray();
		Arrays.sort(chars);
		return new String(chars);
	}

	private static int similarity(String left, String right)
	{
		if (left.contains(right) || right.contains(left))
		{
			return Math.min(left.length(), right.length()) * 4;
		}
		int common = 0;
		String[] leftWords = left.split(" ");
		for (String word : leftWords)
		{
			if (word.length() > 2 && right.contains(word))
			{
				common += word.length();
			}
		}
		return common;
	}

	private static final class AnagramTarget
	{
		private final String signature;
		private final ClueSolution solution;

		private AnagramTarget(String signature, ClueSolution solution)
		{
			this.signature = signature;
			this.solution = solution;
		}
	}

	private static final class ScoredText
	{
		private final String text;
		private final int score;

		private ScoredText(String text, int score)
		{
			this.text = text;
			this.score = score;
		}
	}
}
