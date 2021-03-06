package rita;

import java.util.*;
import java.util.stream.Collectors;

public class Tagger { // TODO: make non-static to match JS, RiTa.tagger

	public static final String[] ADJ = { "jj", "jjr", "jjs" };
	public static final String[] ADV = { "rb", "rbr", "rbs", "rp" };
	public static final String[] NOUNS = { "nn", "nns", "nnp", "nnps" };
	public static final String[] VERBS = { "vb", "vbd", "vbg", "vbn", "vbp", "vbz" };

	////////////////////////////////////////////////////////////////////////

	public String tagInline(String words) {
		return tagInline(words, false);
	}

	public String tagInline(String[] words) {
		return tagInline(words, false);
	}

	public String tagInline(String words, Map<String, Object> opts) {
		boolean simple = Util.boolOpt("simple", opts);
		return tagInline(words, simple);
	}

	public String tagInline(String[] words, Map<String, Object> opts) {
		boolean simple = Util.boolOpt("simple", opts);
		return tagInline(words, simple);
	}

	public String tagInline(String words, boolean useSimpleTags) {
		if (words == null || words.length() < 1) return "";
		return tagInline(Tokenizer.tokenize(words), useSimpleTags);
	}

	public String tagInline(String[] words, boolean useSimpleTags) {
		if (words == null || words.length == 0) return "";
		String[] tags = tag(words, useSimpleTags);
		if (words.length != tags.length) throw new RuntimeException(
				"Tagger: invalid state:" + Arrays.toString(words));

		String delimiter = "/";
		String sb = "";
		for (int i = 0; i < words.length; i++) {
			sb += words[i];
			if (!RiTa.isPunct(words[i])) {
				sb += delimiter + tags[i];
			}
			sb += ' ';
		}
		return sb.trim();
	}

	////////////////////////////////////////////////////////////////////////

	public String[] tag(String words) {
		return tag(words, false);
	}

	public String[] tag(String[] words) {
		return tag(words, false);
	}

	public String[] tag(String words, Map<String, Object> opts) {
		return tag(words, Util.boolOpt("simple", opts));
	}

	public String[] tag(String[] words, Map<String, Object> opts) {
		return tag(words, Util.boolOpt("simple", opts));
	}

	public String[] tag(String words, boolean useSimpleTags) {
		if (words == null || words.length() < 1) return new String[0];
		return tag(Tokenizer.tokenize(words), useSimpleTags);
	}

	public String[] tag(String[] wordsArr, boolean useSimpleTags) {

		if (wordsArr == null || wordsArr.length == 0) {
			return new String[0];
		}

		boolean dbug = false;

		String[][] choices2d = new String[wordsArr.length][];
		String[] result = new String[wordsArr.length];

		for (int i = 0; i < wordsArr.length; i++) {
			String word = wordsArr[i];

			if (word.length() < 1) {
				result[i] = "";
				continue;
			}

			if (word.length() == 1) {

				result[i] = handleSingleLetter(word);
				continue;
			}
			else {
				choices2d[i] = allTags(word); // all options
				if (dbug) System.out.println(word + " " + choices2d[i].length);
				result[i] = choices2d[i][0]; // first option
			}
		}

		// Adjust pos according to transformation rules
		String[] tags = _applyContext(wordsArr, result, choices2d);
		if (dbug) System.out.println("choices2d : " + choices2d);
		if (dbug) System.out.println(tags.length);
		if (useSimpleTags) {
			for (int i = 0; i < tags.length; i++) {
				if (Arrays.asList(NOUNS).contains(tags[i])) {
					tags[i] = "n";
				}
				else if (Arrays.asList(VERBS).contains(tags[i])) {
					tags[i] = "v";
				}
				else if (Arrays.asList(ADJ).contains(tags[i])) {
					tags[i] = "a";
				}
				else if (Arrays.asList(ADV).contains(tags[i])) {
					tags[i] = "r";
				}
				else {
					tags[i] = "-"; // default: other
				}
			}
			if (dbug) System.out.println("simple: " + Arrays.toString(tags));
		}
		if (dbug) System.out.println("Tags : " + Arrays.toString(tags));

		return (tags == null) ? new String[] { } : tags;
	}

	public boolean isAdjective(String word) {
		return checkType(word, ADJ);
	}

	public boolean isAdverb(String word) {
		return checkType(word, ADV);
	}

	public boolean isNoun(String word) {
		boolean result = checkType(word, NOUNS);
		if (!result) {
			String singular = RiTa.singularize(word);
			if (!singular.equals(word)) {
				result = checkType(singular, NOUNS);
			}
		}
		return result;
	}

	public boolean isVerb(String word) {
		return checkType(word, VERBS);
	}

	public boolean isVerbTag(String tag) {
		return Arrays.asList(VERBS).contains(tag);
	}

	public boolean isNounTag(String tag) {
		return Arrays.asList(NOUNS).contains(tag);
	}

	public boolean isAdverbTag(String tag) {
		return Arrays.asList(ADV).contains(tag);
	}

	public boolean isAdjTag(String tag) {
		return Arrays.asList(ADJ).contains(tag);
	}

	////////////////////////////////////////////////////////////////////////

	private String[] _applyContext(String[] words, String[] result, String[][] choices2d) {

		// console.log("ac(" + words + "," + result + "," + choices2d + ")");
		boolean dbug = false;

		// Apply transformations
		for (int i = 0, l = words.length; i < l; i++) {

			String word = words[i];
			String tag = result[i];
			String[] results = result;
			// transform 1a: DT, {VBD | VBP | VB} --> DT, NN
			if (i > 0 && (results[i - 1].equals("dt"))) {

				if (tag.startsWith("vb")) {
					tag = "nn";

					// transform 7: if a word has been categorized as a
					// common noun and it ends with "s", then set its type to plural common noun
					// (NNS)

					if (word.matches("^.*[^s]s$")) {
						if (!Arrays.asList(Util.MASS_NOUNS).contains(word)) {
							tag = "nns";
						}
					}

					if (dbug) logCustom("1a", word, tag);
				}

				// transform 1b: DT, {RB | RBR | RBS} --> DT, {JJ |
				// JJR | JJS}
				else if (tag.startsWith("rb")) {

					tag = (tag.length() > 2) ? "jj" + tag.charAt(2) : "jj";
					if (dbug) logCustom("1b", word, tag);
				}
			}

			// transform 2: convert a noun to a number (cd) if it is
			// all digits and/or a decimal "."
			if (tag.startsWith("n") && choices2d[i].length != 0) {
				if (Util.isNum(word)) {
					tag = "cd";
				} // mods: dch (add choice check above) <---- ? >
			}

			// transform 3: convert a noun to a past participle if
			// word ends with "ed" and (following any nn or prp?)
			if (i > 0 && tag.startsWith("n") && word.endsWith("ed") && !word.endsWith("eed")
					&& results[i - 1].matches("^(nn|prp)$")) {
				tag = "vbn";
			}

			// transform 4: convert any type to adverb if it ends in "ly";
			if (word.endsWith("ly")) {
				tag = "rb";
			}

			// transform 5: convert a common noun (NN or NNS) to a
			// adjective if it ends with "al", special-case for mammal
			if (word.length() > 4 && tag.startsWith("nn") && word.endsWith("al") && !word.equals("mammal")) {
				tag = "jj";
			}

			// transform 6: convert a noun to a verb if the
			// preceeding word is modal
			if (i > 0 && tag.startsWith("nn") && results[i - 1].startsWith("md")) {
				tag = "vb";
			}

			//transform 7(dch): convert a vb to vbn when following vbz/'has'  (She has ridden, He has rode)
			if (tag.equals("vbd") && i > 0 && result[i - 1].matches("^(vbz)$")) {
				tag = "vbn";
				if (dbug) logCustom("7", word, tag);
			}

			// transform 8: convert a common noun to a present
			// participle verb (i.e., a gerund)
			if (tag.startsWith("nn") && word.endsWith("ing")) {

				// DH: fixed here -- add check on choices2d for any verb: eg. // "morning"
				if (hasTag(choices2d[i], "vb")) {
					tag = "vbg";
					if (dbug) logCustom("8", word, tag);
				}
			}

			// transform 9(dch): convert plural nouns (which are also 3sg-verbs) to
			// 3sg-verbs when following a singular noun (the dog dances, Dave dances, he
			// dances)
			if (i > 0 && tag.equals("nns") && hasTag(choices2d[i], "vbz") && results[i - 1].matches("^(nn|prp|nnp)$")) {
				tag = "vbz";
				if (dbug) logCustom("9", word, tag);
			}

			// transform 10(dch): convert common nouns to proper
			// nouns when they start w' a capital

			if (tag.startsWith("nn") && (word.charAt(0) == Character.toUpperCase(word.charAt(0)))) {
				// if it is not at the start of a sentence or it is the only word
				// or when it is at the start of a sentence but can't be found in the dictionary
				if (i != 0 || words.length == 1 || (i == 0 && !lexHas("nn", RiTa.singularize(word).toLowerCase()))) {
					tag = tag.endsWith("s") ? "nnps" : "nnp";
					if (dbug) logCustom("10", word, tag);
				}
			}

			// transform 11(dch): convert plural nouns (which are
			// also 3sg-verbs) to 3sg-verbs when followed by adverb
			if (i < result.length - 1 && tag.equals("nns") && results[i + 1].startsWith("rb") &&
					hasTag(choices2d[i], "vbz")) {
				tag = "vbz";
				if (dbug) logCustom("11", word, tag);
			}

			// transform 12(dch): convert plural nouns which have an entry for their base
			// form to vbz
			if (tag.equals("nns")) {

				// is preceded by one of the following
				String[] options = new String[] { "nn", "prp", "cc", "nnp" };
				List<String> list = Arrays.asList(options);
				if (i > 0 && list.contains(results[i - 1])) {
					// if word is ends with s or es and is "nns" and has a vb
					if (lexHas("vb", RiTa.singularize(word))) {
						tag = "vbz";
						if (dbug) logCustom("12", word, tag);
					}
				} // if only word and not in lexicon
				else if (words.length == 1 && choices2d[i].length == 0) {
					// if the stem of a single word could be both nn and vb, return nns
					// only return vbz when the stem is vb but not nn
					if (!lexHas("nn", RiTa.singularize(word)) && lexHas("vb", RiTa.singularize(word))) {
						tag = "vbz";
						if (dbug) logCustom("12", word, tag);
					}

				}
			}

			// transform 13(cqx): convert a vb/ potential vb to vbp when following nns
			// (Elephants dance, they dance)
			if (tag.equals("vb") || (tag.equals("nn") && hasTag(choices2d[i], "vb"))) {
				if (i > 0 && results[i - 1].matches("^(nns|nnps|prp)$")) {
					tag = "vbp";
					if (dbug) logCustom("13", word, tag);
				}
			}

			// webIssue#83 sequential adjectives(jc): (?:dt)? (?:jj)* (nn) (?:jj)* nn && $1 can be tagged as jj-> $1 convert to jj (e.g a light bule sky)
			if (tag.equals("nn") && i < l - 1 && Arrays.asList(Arrays.copyOfRange(result, i + 1, result.length)).contains("nn")) {
				int idx = Arrays.asList(Arrays.copyOfRange(result, i + 1, result.length)).indexOf("nn");
				Boolean allJJ = true; // between nn and nn are are jj
				for (int k = 0; k < idx; k++) {
					if (!result[i + 1 + k].equals("jj")) {
						allJJ = false;
						break;
					}
				}
				if (allJJ && Arrays.asList(this.allTags(word)).contains("jj")) {
					tag = "jj";
				}
			}

			results[i] = tag;
		}

		return result;
	}

	/*
	 * Return the array of all pos tags from the lexicon,
	 * or the best guess(es) if not found.
	 */
	public String[] allTags(String word) {
		return allTags(word, false);
	}

	/*
	 * Return the array of all pos tags from the lexicon,
	 * or the best guess(es) if not found, unless if noDerivations
	 * is true, in which case null is returned if the word is not
	 * in the lexicon
	 */
	public String[] allTags(String word, boolean noDerivations) {
		String[] posdata = RiTa.lexicon().posArr(word);
		// System.out.println("data : " + Arrays.toString(posdata));
		if (posdata.length == 0) posdata = derivePosData(word);
		if (posdata.length == 0) throw new RuntimeException("Unable to derive pos data for: " + word);
		return posdata;
	}

	String[] derivePosData(String word) {
		/*
		 * Try for a verb or noun inflection VBD Verb, past tense VBG Verb, gerund or
		 * present participle VBN Verb, past participle VBP Verb, non-3rd person
		 * singular present VBZ Verb, 3rd person singular present NNS Noun, plural
		 */
		String[] pos;
		Lexicon lexicon = RiTa.lexicon();

		if (word.endsWith("ies")) { // 3rd-person sing. present (satisfies, falsifies)

			String check = word.substring(0, word.length() - 3) + "y";
			pos = lexicon.posArr(check);

			if (Arrays.asList(pos).contains("vb")) return new String[] { "vbz" };
		}
		else if (word.endsWith("s")) { // plural noun or vbz

			List<String> result = new ArrayList<String>();

			// remove suffix (s) and test (eg 'hates', 'cakes')
			checkPluralNounOrVerb(word.substring(0, word.length() - 1), result);

			if (word.endsWith("es")) {

				// remove suffix (es) and test (eg 'repossesses')
				checkPluralNounOrVerb(word.substring(0, word.length() - 2), result);

				// singularize and test (eg 'thieves')
				checkPluralNounOrVerb(RiTa.singularize(word), result);
			}
			
			if (result.size() > 0) return result.toArray(new String[result.size()]);

		}
		else if (word.endsWith("ed")) { // simple past or past participle

			pos = lexicon.posArr(word.substring(0, word.length() - 1));
			if (pos.length < 1) pos = lexicon.posArr(word.substring(0, word.length() - 2));
			if (Arrays.asList(pos).contains("vb")) {
				return new String[] { "vbd", "vbn" }; // hate-> hated || row->rowed
			}

		}
		else if (word.endsWith("ing")) {

			String stem = word.substring(0, word.length() - 3);
			if (stem.length() > 0) {
				pos = lexicon.posArr(stem);
				if (Arrays.asList(pos).contains("vb")) {
					return new String[] { "vbg" }; // assenting
				}
				else {
					pos = lexicon.posArr(stem + 'e'); // hate
					if (Arrays.asList(pos).contains("vb")) {
						return new String[] { "vbg" }; // hating
					}
				}
			}
		}

		// Check if this could be a plural noun form
		if (isLikelyPlural(word)) {
			return new String[] { "nns" };
		}

		if (word.equals("the") || word.equals("a")) {
			return new String[] { "dt" };
		}

		// Give up with a best guess
		if (word.endsWith("ly")) {
			return new String[] { "rb" };
		}

		return new String[] { word.endsWith("s") ? "nns" : "nn" };
	}

	private static void logCustom(String i, String frm, String to) {
		System.out.println("\n  Custom(" + i + ") tagged '" + frm + "' -> '" + to + "'\n\n");
	}

	private static boolean hasTag(String[] choices, String tag) {
		// if (!Array.isArray(choices)) return false;
		String choiceStr = String.join("", choices);
		return (choiceStr.indexOf(tag) > -1);
	}

	private boolean lexHas(String pos, String word) {
		String[] tags = RiTa.lexicon().posArr(word);
		if (tags.length < 1) return false;
		for (int j = 0; j < tags.length; j++) {
			if (pos.equals(tags[j])) return true;
			if (pos.equals("n") && isNounTag(tags[j]) ||
					pos.equals("v") && isVerbTag(tags[j]) ||
					pos.equals("r") && isAdverbTag(tags[j]) ||
					pos.equals("a") && isAdjTag(tags[j])) {
				return true;
			}
		}
		return false;
	}

	private static String handleSingleLetter(String c) {
		String result = c;
		if (c.equals("a") || c.equals("A"))
			result = "dt";
		else if (c.equals("I"))
			result = "prp";
		else if (Util.isNum(c))
			result = "cd";
		return result;
	}

	private static void checkPluralNounOrVerb(String stem, List<String> result) {
		List<String> pos = Arrays.asList(RiTa.lexicon().posArr(stem));
		if (pos.size() > 0) {
			if (pos.contains("nn")) result.add("nns"); // ?? any case
			if (pos.contains("vb")) result.add("vbz");
		}
	}

	private boolean isLikelyPlural(String word) {
		return lexHas("n", RiTa.singularize(word)) || Inflector.isPlural(word);
	}

	private boolean checkType(String word, String[] tagArray) {
		return Arrays.asList(allTags(word)).stream()
				.filter(p -> Arrays.asList(tagArray).contains(p))
				.collect(Collectors.toList()).size() > 0;
	}

	public static void main(String[] args) {
		System.out.println(Arrays.asList(new Tagger().tag("Bad boy")));
	}
}
