// CS124 Final Project Code (PA6)
// Direct MT Translation System
// Carlos Girod (cgirod3@stanford.edu), Kiana Hui (kianah@stanford.edu), Alec Powell (atpowell@stanford.edu), Hailey Spelman (spelman@stanford.edu)
// Winter 2015

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.*;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Translator {

	private static final String TAGGER_FILE_ENG = "stanford-postagger-full-2015-01-30/models/english-left3words-distsim.tagger";
	private static final String TAGGER_FILE_SPA = "stanford-postagger-full-2015-01-30/models/spanish-distsim.tagger";
	private Map<String, List<String>> dictionaryMap = new HashMap<String, List<String>>();
	private Map<String, Double> dictionaryFreqs = new HashMap<String, Double>(); 
	private	static List<String> subjectVerbs = new ArrayList<String>(Arrays.asList("are", "is", "had", "have", "were", "was", "am", "has", "should", "must", "could", "would"));

	public void buildDictionary(String fileStr, String freqStr) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(fileStr)));
			while(true) {
				String line = br.readLine();
				if(line == null) break;
				int numTranslations = Integer.parseInt(line);
				String key = br.readLine().toLowerCase();
				List<String> translationList = new ArrayList<String>();
				for(int i = 0; i < numTranslations; i++) {
					String trans = br.readLine().toLowerCase();
					translationList.add(trans);
				}
				dictionaryMap.put(key, translationList);
			}
			
		}catch (FileNotFoundException e) {
			System.out.println("File \"" + fileStr + "\" Not Found.");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error reading from file \"" + fileStr + "\".");
			e.printStackTrace();
			System.exit(1);
		}catch (NumberFormatException e) {
			System.out.println("Error parsing int from first line of file \"" + fileStr + "\".");
			e.printStackTrace();
			System.exit(1);
		}finally {
			try{
				if (br != null) br.close();
			}
			catch(IOException e) {
				System.out.println("Error closing buffered reader");
				e.printStackTrace();
				System.exit(1);
			}
		}

		//build dictionaryFreqs map too!
		try {
			br = new BufferedReader(new FileReader(new File(freqStr)));
			while(true) {
				String line = br.readLine();
				if (line == null) break;
				line = line.toLowerCase();
				double ngramFreq = Double.parseDouble(br.readLine());
				dictionaryFreqs.put(line, ngramFreq);
			}
		} catch(IOException e) {
			System.out.println("I'm sorry, we couldn't open the file.");
			System.exit(1);
		}
	}

	public List<String> phraseReplace(List<String> sentences, String phrasesFile) {
		Map<String, String> phraseMap =  new HashMap<String, String>();
		BufferedReader br = null;
		List<String> newSentences = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(new File(phrasesFile)));
			while(true) {
				String spanishLine = br.readLine();
				if (spanishLine == null) break;
				String translation = br.readLine();
				if (translation == null) break;
				// line = line.toLowerCase();
				phraseMap.put(spanishLine, translation);
			}
		} catch(IOException e) {
			System.out.println("I'm sorry, we couldn't open the file.");
			System.exit(1);
		}
		for (String sentence: sentences) {
			for (String phrase: phraseMap.keySet()) {
				if (sentence.contains(phrase)) {
					sentence = sentence.replaceAll(phrase, phraseMap.get(phrase));
					// System.out.println("REPLACED PHRASE: " + sentence);
				}
			}
			newSentences.add(sentence);
		}
		return newSentences;
	}

	public List<String> spanishPOS(List<String> sentences) {
		List<String> spanishTaggedSentences = tagger(sentences, TAGGER_FILE_SPA);
		// for(String s: spanishTaggedSentences) {
		// 	System.out.println("..." + s);
		// }
		return spanishTaggedSentences;
	}

	public List<String> translate(String fileStr, List<String> newSpanishTagged) {
		List<String> sentences = new ArrayList<String>();
		List<String> translations = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(fileStr)));
			while(true) {
				String line = br.readLine();
				if (line == null) break;
				//line = line.toLowerCase();
				char firstLetter = Character.toLowerCase(line.charAt(0));
				line = firstLetter + line.substring(1);
				sentences.add(line);
			}
		} catch(IOException e) {
			System.exit(1);
		}
		List<String> newSentences = new ArrayList<String>();
		for(String sentence : sentences) {
			String commas = sentence.replaceAll(",", " ,");
			newSentences.add(commas);
		}

		List<String> phrasesReplaced = phraseReplace(newSentences, "phrases.txt");
		List<String> spanishTaggedSentences = spanishPOS(phrasesReplaced);
		for(int i = 0; i < sentences.size(); i++) {
			// System.out.println(spanishTaggedSentences.get(i));
			translations.add(translateLineSpanEng(phrasesReplaced.get(i), spanishTaggedSentences.get(i), newSpanishTagged));
		}
		return translations;
	}

	public String convertPOS(String spPOS) {
		if (spPOS.charAt(0) == 'a') {
			return "_ADJ";
		}else if (spPOS.charAt(0) == 'n') {
			return "_NOUN";
		}else if (spPOS.charAt(0) == 'v') {
			return "_VERB";
		}else {
			return "_IGN";
		}
	}

	public String translateLineSpanEng(String spReg, String spTagged, List<String> newSpanishTagged) {
		// System.out.println(spReg);
		// System.out.println(spTagged);
		// System.out.println("-----");
		List<String> finalTranslation = new ArrayList<String>();
		//remove punctuation
		String[] tokens = spReg.replaceAll("[.]", "").split("\\s+");
		// System.out.println(tokens.length);
		String[] taggedTokens = spTagged.replaceAll("[.]","").split("\\s+");
		String spanishSentence = "";
		for(int i = 0; i < tokens.length; i++) {
			// System.out.println("Spanish Tagged: " + taggedTokens[i]);
			String extractedTag = taggedTokens[i].substring(taggedTokens[i].indexOf('_')+1);
			String newSpTagged = taggedTokens[i].substring(0, taggedTokens[i].indexOf('_')) + convertPOS(extractedTag);
			spanishSentence += newSpTagged + " ";
			// System.out.println(taggedTokens[i]);
			// System.out.println(newSpTagged);
			// System.out.println(extractedTag);
			boolean hasComma = false;
			// String mostLikelyTrans = "??";
			String mostLikelyTrans = tokens[i];
			double value = 0.0;
			if(tokens[i].contains(",")) {
				hasComma = true;
				tokens[i] = tokens[i].substring(0, tokens[i].length()-1);
			}
			if(dictionaryMap.get(tokens[i]) != null) {
				for(String transWord : dictionaryMap.get(tokens[i])) {
					if(dictionaryFreqs.get(transWord) == null) {
						// System.out.println("No freq count for " + transWord);
						continue;
					}
					if(dictionaryFreqs.get(transWord) > value) {
						mostLikelyTrans = transWord;
						value = dictionaryFreqs.get(mostLikelyTrans);
					}
				}
			} else {
				if((tokens[i].equals("del")) || (tokens[i].equals("al"))) {
					String firstWord = tokens[i].substring(0,tokens[i].indexOf('l'));
					String secondWord = "e" + tokens[i].substring(tokens[i].indexOf('l'));
					mostLikelyTrans = dictionaryMap.get(firstWord).get(0) + " " + dictionaryMap.get(secondWord).get(0);
				} else
				// mostLikelyTrans = "??";
				mostLikelyTrans = tokens[i];
			}
			finalTranslation.add(mostLikelyTrans);
			if(hasComma) {
				finalTranslation.add(",");
			}
		}
		newSpanishTagged.add(spanishSentence);
		// allPositions.add(positions);
		String toReturn = "";
		for(String token : finalTranslation) {
			toReturn += token + " ";
		}
		return toReturn;
	}

	public List<String> tagger(List<String> sentences, String taggerFile) {
		MaxentTagger tagger = new MaxentTagger(taggerFile);
		List<String> toReturn = new ArrayList<String>();
		for(String sentence : sentences) {
			String taggedSentence = tagger.tagTokenizedString(sentence);
    		// System.out.println(taggedSentence);
			toReturn.add(taggedSentence);
		}
		return toReturn;
	}

	private int numSyllables(String word) {
	    int count = 0;
	    word = word.toLowerCase();
	    for (int i = 0; i < word.length(); i++) {
	        if (word.charAt(i) == '\"' || word.charAt(i) == '\'' || word.charAt(i) == '-' || word.charAt(i) == ',' || word.charAt(i) == ')' || word.charAt(i) == '(') {
	            word = word.substring(0,i)+word.substring(i+1, word.length());
	        }
	    }
	    boolean isPrevVowel = false;
	    for (int j = 0; j < word.length(); j++) {
	        if (word.contains("a") || word.contains("e") || word.contains("i") || word.contains("o") || word.contains("u")) {
	            if (isVowel(word.charAt(j)) && !((word.charAt(j) == 'e') && (j == word.length()-1))) {
	                if (isPrevVowel == false) {
	                    count++;
	                    isPrevVowel = true;
	                }
	            } else {
	                isPrevVowel = false;
	            }
	        } else {
	            count++;
	            break;
	        }
	    }
	    return count;
	}

	private boolean isVowel(char c) {
        return "aeiou".indexOf(c) > -1;
    }

    private String getNewAdj(String word, int syl, String type) {
    	if (type.equals("more") || type.equals("More")) {
    		if (syl == 2) {
    			word = word.substring(0, word.length()-1) + "ier";
    		}else if (word.charAt(word.length() - 1) == 'e') {
    			word += "r";
    		}else if (!isVowel(word.charAt(word.length() - 1)) && isVowel(word.charAt(word.length() - 2))) {
    			word += word.charAt(word.length() - 1) + "er";
    		}else {
    			word += "er";
    		}
    	}else {
    		if (syl == 2) {
    			word = word.substring(0, word.length()-1) + "iest";
    		}else if (word.charAt(word.length() - 1) == 'e') {
    			word += "st";
    		}else if (!isVowel(word.charAt(word.length() - 1)) && isVowel(word.charAt(word.length() - 2))) {
    			word += word.charAt(word.length() - 1) + "est";
    		}else {
    			word += "est";
    		}
    	}
    	return word;
    }

	public List<String> superlatives(List<String> old) {
		List<String> translations = new ArrayList<String>();
		for (String s: old) {
			 translations.add(s.replaceAll("-", " "));
		}
		List<String> tagged = tagger(translations, TAGGER_FILE_ENG);
		List<String> fixed = new ArrayList<String>();
		for (int x = 0; x < translations.size(); x++) {
			String[] tokens = translations.get(x).split(" ");
			String[] tagTokens = tagged.get(x).split(" ");
			String merged = "";
			for (int i = 0; i < tokens.length; i++) {
				boolean added = false;
				if (i != tokens.length - 1 && (tokens[i].equals("more") || tokens[i].equals("More") || tokens[i].equals("most") || tokens[i].equals("Most"))) {
					String posNext = tagTokens[i+1].substring(tagTokens[i+1].indexOf("_") + 1);
					if (posNext.equals("JJ")) {
						int syl = numSyllables(tokens[i+1]);
						if (syl == 1 || (syl == 2 && tokens[i+1].charAt(tokens[i+1].length() - 1) == 'y')) {
							String append = getNewAdj(tokens[i+1], syl, tokens[i]);
							merged += append + " ";
							added = true;
						}
					}
				}
				if (added) {
					i++;
				}else {
					merged += tokens[i] + " ";
				}
			}
			fixed.add(merged);
		}
		return fixed;
	}


	private String getTag(String alreadyTaggedStr) {
		return alreadyTaggedStr.substring(alreadyTaggedStr.indexOf('_')+1).trim();
	}

	private String getWord(String alreadyTaggedStr) {
		return alreadyTaggedStr.substring(0, alreadyTaggedStr.indexOf('_')).trim();
	}

	private static List<String> cconjunctions = new ArrayList<String>(Arrays.asList("and","nor","but","or","yet"));
	public List<String> processPOS(List<String> taggedSentences) {
		List<String> toReturn = new ArrayList<String>();
		for(String sent : taggedSentences) {
			String finalStr = "";
			String[] tagTokens = sent.split("\\s+");
			for(int i = 0; i < tagTokens.length; i++) {
				// String pos = tagTokens[i].substring(tagTokens[i].indexOf('_')+1);
				// String actualWord = tagTokens[i].substring(0, tagTokens[i].indexOf('_'));
				String pos = getTag(tagTokens[i]);
				String actualWord = getWord(tagTokens[i]);
				if((!pos.equals("NN") && !pos.equals("NNS")) || (i == tagTokens.length-1)) {
					finalStr += actualWord + " ";
				} else {
					System.out.println("Noun: " + actualWord);
					String posNext = tagTokens[i+1].substring(tagTokens[i+1].indexOf('_')+1);
					if(posNext.equals("JJ")) {
						int numChanged = 1;
						boolean andFound = false;
						List<String> adjectives = new ArrayList<String>();
						while(numChanged+i < tagTokens.length) {
							String posNextNext = tagTokens[i+numChanged].substring(tagTokens[i+numChanged].indexOf('_')+1);
							if(!posNextNext.equals("JJ") && !posNextNext.equals("CC"))
								break;
							String nextActualWord = tagTokens[i+numChanged].substring(0, tagTokens[i+numChanged].indexOf('_'));
							System.out.println("Next: " + nextActualWord);
							if(posNextNext.equals("CC")) {
								if(nextActualWord.equals("for") || nextActualWord.equals("so"))
									break;
								andFound = true;
							}
							adjectives.add(nextActualWord);
							numChanged++;
						}
						if(andFound) {
							if(cconjunctions.contains(adjectives.get(adjectives.size()-1))) {
								adjectives.remove(adjectives.size()-1);
								numChanged--;
							}
							for(String adj : adjectives) {
								finalStr += adj + " ";
							}
						} else {
							for(int j = adjectives.size()-1; j >= 0; j--) {
								finalStr += adjectives.get(j) + " ";
							}
						}
						finalStr += actualWord + " ";
						i = numChanged+i-1;
					} else {
						finalStr += actualWord + " ";
					}
				}
			}
			toReturn.add(finalStr);
		}
		return toReturn;
	}

	private List<String> addSubjects (List<String> translations, List<String> englishTagged) {
		List<String> exceptions = new ArrayList<String>(Arrays.asList("here", "there", "that", "should", "would", "could", "had", "has", "not"));
		List<String> newTranslations = new ArrayList<String>();
		for (int i = 0; i < translations.size(); i++) {
			String newSentence = "";
			String[] tokens = translations.get(i).split(" ");
			String[] sentencePos = englishTagged.get(i).split(" ");

			// iterate through every token in the sentence to search for our verbs
			for (int j = 0; j < tokens.length; j++) {
				String toAdd = tokens[j];
				String phraseCheck = toAdd.replaceAll("-", " ");
				String[] phrase = phraseCheck.split(" ");
				String pos = sentencePos[j].substring(sentencePos[j].indexOf('_')+1);
				if (subjectVerbs.contains(toAdd) || subjectVerbs.contains(phrase[0])) {
					if (j == 0 && !pos.equals("VBG")) {
						toAdd = "He " + toAdd;
					} else if (j != 0) {
						String prevPos = sentencePos[j-1].substring(sentencePos[j-1].indexOf('_')+1);
						if (!prevPos.equals("NN") && !prevPos.equals("NNS") && !prevPos.equals("PRP") && !prevPos.equals("PRP$") && 
							(!exceptions.contains(tokens[j-1]))) {
							toAdd = " he " + toAdd;
						}
					}
				}
				newSentence += (toAdd + " ");
			}
			newTranslations.add(newSentence);
			}
		return newTranslations;
	}

	private static List<String> subjects = new ArrayList<String>(Arrays.asList("i","he","she","they","we"));
	private static List<String> prepositions = new ArrayList<String>(Arrays.asList("of","to","on","at","for","before","past","by","on","under","below","over","above","across","through","into","towards","onto","from","off","about"));
	private static int containsPronounToChange(String str) {
		String[] tokens = str.split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			if (subjects.contains(tokens[i])) {
				if (i != 0) {
					if (prepositions.contains(tokens[i-1])) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	public List<String> pronounAgreement(List<String> oldTranslations) {
		List<String> translationsToReturn = new ArrayList<String>();
		Map<String,String> pronounReplacements = new HashMap<String,String>()
		{{
			put("i","me");
			put("he","him");
			put("she","her");
			put("they","them");
			put("we","us");
		}};
		for (String translation : oldTranslations) {
			String[] tokens = translation.split("\\s+");
			int possibleIdx = containsPronounToChange(translation);
			if (possibleIdx > 0) {
				//changes it
				tokens[possibleIdx] = pronounReplacements.get(tokens[possibleIdx]);
			}
			String toAdd = "";
			for(int i = 0; i < tokens.length; i++) {
				toAdd += tokens[i] + " ";
			}
			translationsToReturn.add(toAdd);
		}
		return translationsToReturn;
	}

	public List<String> possessions(List<String> oldTranslations) {
		MaxentTagger tagger = new MaxentTagger(TAGGER_FILE_ENG);
		List<String> toReturn = new ArrayList<String>();
		Pattern possessionPattern = Pattern.compile("the ((\\w+) of the (\\w+))");
		for (String sentence : oldTranslations) {
			Matcher possMatcher = possessionPattern.matcher(sentence);
			while (possMatcher.find()) {
				String str2 = possMatcher.group(2);
				String str1 = possMatcher.group(3);
				String taggedStr1 = tagger.tagString(str1);
				String taggedStr2 = tagger.tagString(str2);
				if (getTag(taggedStr1).equals("NN") && (getTag(taggedStr2).equals("NN") || getTag(taggedStr2).equals("NNS"))) {
					String replacement = str1 + "'s " + str2;
					// System.out.println(replacement);
					sentence = sentence.replaceAll(possMatcher.group(1), replacement);
				}
			}
			toReturn.add(sentence);
		}
		return toReturn;
	}

	public List<String> infinitiveVerbs(List<String> oldTranslations) {
		List<String> reTag = new ArrayList<String>();
		for (String s: oldTranslations) {
			 reTag.add(s.replaceAll("-", " "));
		}
		List<String> englishTagged = tagger(reTag, TAGGER_FILE_ENG);
		List<String> toReturn = new ArrayList<String>();
		for (int x = 0; x < englishTagged.size(); x++) {
			String[] tokens = englishTagged.get(x).split("\\s+");
			String newSentence = "";
			for (int i = 0; i < tokens.length; i++) {
				String originalToken = getWord(tokens[i]) + " ";
				String changedInfinitive = "";
				boolean addedTo = false;
				boolean changedOf = false;
				if (i <= tokens.length - 2) {
					String word1Tag = getTag(tokens[i]);
					String word2Tag = getTag(tokens[i+1]);
					// System.out.println(getWord(tokens[i]) + ":" + word1Tag);
					// System.out.println(getWord(tokens[i+1]) + ":" + word2Tag);
					if(word1Tag.equals("VBD") && (word2Tag.equals("VBN") || word2Tag.equals("VB")) && !subjectVerbs.contains(getWord(tokens[i])) ) {
						changedInfinitive = getWord(tokens[i]) + " to " + getWord(tokens[i+1]) + " ";
						addedTo = true;
						i += 1;
					} 
				} 

				// check for form VERB of VERB
				if (i <= tokens.length - 3) {
					String word1Tag = getTag(tokens[i]);
					String word2 = getWord(tokens[i+1]);
					String word3Tag = getTag(tokens[i+2]);
					// System.out.println(getWord(tokens[i]) + ":" + word1Tag);
					// System.out.println(getWord(tokens[i+1]) + ":" + getTag(tokens[i+1]));
					// System.out.println(getWord(tokens[i+2]) + ":" + word3Tag);
					if (word1Tag.equals("VBD") && (word3Tag.equals("VBN") || word3Tag.equals("VB") || word3Tag.equals("VBP")) 
						&& !subjectVerbs.contains(getWord(tokens[i])) && word2.equals("of")) {
						changedInfinitive = getWord(tokens[i]) + " to " + getWord(tokens[i+2]) + " ";
						changedOf = true;
						i += 2;
					}
				} 
				if (!addedTo && !changedOf) {
					changedInfinitive += originalToken;
				}
				newSentence += changedInfinitive;
			}
			toReturn.add(newSentence);
		}
		return toReturn;
	}

	public List<String> negationFix(List<String> prevTranslations) {
		List<String> reTag = new ArrayList<String>();
		for (String s: prevTranslations) {
			 reTag.add(s.replaceAll("-", " "));
		}
		List<String> englishTagged = tagger(reTag, TAGGER_FILE_ENG);
		List<String> toReturn = new ArrayList<String>();
		List<String> translationsToReturn = new ArrayList<String>();
		for (int x = 0; x < englishTagged.size(); x++) {
			String[] tokens = englishTagged.get(x).split("\\s+");
			String newSentence = "";
			for (int i = 0; i < tokens.length; i++) {
				if (i <= tokens.length - 2) {
					String word1Tag = getTag(tokens[i]);
					String word2 = getWord(tokens[i+1]);
					if ((word2.equals("not") || word2.equals("no")) && word1Tag.charAt(0) != 'V') {
						int numMoved = 2;
						String tagCheck = getTag(tokens[i+1]);
						String move = getWord(tokens[i]) + " ";
						String notMoved= getWord(tokens[i]) + " " + getWord(tokens[i+1]) + " ";
						while (tagCheck.charAt(0) != 'V' && (i + numMoved) < tokens.length) {
							tagCheck = getTag(tokens[i+numMoved]);
							move += getWord(tokens[i+numMoved]) + " ";
							notMoved += getWord(tokens[i+numMoved]) + " ";
							if (tagCheck.charAt(0) == 'V'){
								move += "not ";
								break;
							}
							numMoved++;
						}
						if (i + numMoved == tokens.length -1) {
							newSentence += notMoved;
						} else {
							newSentence += move;
						}
						i += numMoved;
					} else {
						newSentence += getWord(tokens[i]) + " ";
					}
				} else {
					newSentence += getWord(tokens[i]) + " ";
				}
			}
			toReturn.add(newSentence);
		}
		return toReturn;
	} 

	private static void outputToFile(List<String> translations, String outputFile) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFile, "UTF-8");
			for (String translation : translations) {
				char upper = Character.toUpperCase(translation.charAt(0));
				translation = upper + translation.substring(1);
				translation = translation.replaceAll(" ,", ",");
				translation = translation.replaceAll("-", " ");
				translation = translation.trim();
				translation += ".";
				writer.println(translation);
			}
			writer.close();

		} catch(FileNotFoundException f) {
			System.out.println("no output file given!");
			System.exit(1);
		} catch(UnsupportedEncodingException e) {
			System.out.println("Encoding not supported.");
			System.exit(1);
		}
	}

	//ARGUMENTS: corpus file, ngrams file, file to translate, output file
	public static void main(String[] args) {
		Translator t = new Translator();
		// String englishTaggerFile = "stanford-postagger-full/models/english-left3words-distsim.tagger";
		// String spanishTaggerFile = "stanford-postagger-full/models/spanish-distsim.tagger";

		t.buildDictionary(args[0], args[1]);
		List<String> newSpanishTagged = new ArrayList<String>();
		List<String> translations = t.translate(args[2], newSpanishTagged);

		System.out.println("We're done translating!");
		System.out.println("Tagging...");

		translations = t.superlatives(translations);

		translations = t.tagger(translations, TAGGER_FILE_ENG);
		translations = t.processPOS(translations);

		List<String> englishTagged = t.tagger(translations, TAGGER_FILE_ENG);
		translations = t.addSubjects(translations, englishTagged);
		//magic happens here
		englishTagged = t.tagger(translations, TAGGER_FILE_ENG);
		translations = t.pronounAgreement(translations);

		englishTagged = t.tagger(translations, TAGGER_FILE_ENG);
		translations = t.possessions(translations);

		// List<String> englishTagged_new = t.tagger(translations, TAGGER_FILE_ENG);
		translations = t.infinitiveVerbs(translations);
		translations = t.negationFix(translations);

		outputToFile(translations, args[3]);

		System.out.println("Final translations:");
		for (String translation : translations) {
			char upper = Character.toUpperCase(translation.charAt(0));
			translation = upper + translation.substring(1);
			translation = translation.replaceAll(" ,", ",");
			translation = translation.replaceAll("-", " ");
			translation = translation.trim();
			translation += ".";
			System.out.println(translation);
		}
		System.out.println("--------");
	}

}
