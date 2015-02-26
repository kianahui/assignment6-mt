// CS124 Final Project Code (PA6)
// Direct MT Translation System
// Carlos Girod (cgirod3@stanford.edu), Kiana Hui (kianah@stanford.edu), Alec Powell (atpowell@stanford.edu), Hailey Spelman (spelman@stanford.edu)
// Winter 2015

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.HashMap;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Translator {

	private Map<String, List<String>> dictionaryMap = new HashMap<String, List<String>>();
	private Map<String, Double> dictionaryFreqs = new HashMap<String, Double>(); 

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

	public List<String> spanishPOS(List<String> sentences, String spTaggerFile) {
		List<String> spanishTaggedSentences = tagger(sentences, spTaggerFile);
		// for(String s: spanishTaggedSentences) {
		// 	System.out.println("..." + s);
		// }
		return spanishTaggedSentences;
	}

	public List<String> translate(String fileStr, String spTaggerFile, List<String> newSpanishTagged, List<ArrayList<Integer>> positions) {
		List<String> sentences = new ArrayList<String>();
		List<String> translations = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(fileStr)));
			while(true) {
				String line = br.readLine();
				if (line == null) break;
				line = line.toLowerCase();
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
		List<String> spanishTaggedSentences = spanishPOS(phrasesReplaced, spTaggerFile);
		for(int i = 0; i < sentences.size(); i++) {
			// System.out.println(spanishTaggedSentences.get(i));
			translations.add(translateLineSpanEng(phrasesReplaced.get(i), spanishTaggedSentences.get(i), newSpanishTagged, positions));
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

	public String translateLineSpanEng(String spReg, String spTagged, List<String> newSpanishTagged, List<ArrayList<Integer>> allPositions) {
		ArrayList<Integer> positions = new ArrayList<Integer>();
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
						System.out.println("No freq count for " + transWord);
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
			String[] splitTrans = mostLikelyTrans.split("\\s+");
			for (String s: splitTrans) positions.add(i);
			if(hasComma) {
				finalTranslation.add(",");
			}
		}
		newSpanishTagged.add(spanishSentence);
		allPositions.add(positions);
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

	private String checkPos(String pos, int index, String spanishSentence) {
		if (posBad(pos, index, spanishSentence)) {
			return fixPos(pos);
		}
		return pos;
	}

	private boolean posBad(String pos, int index, String spanishSentence) {
		// System.out.println(spanishSentence);
		// System.out.println(index);
		String[] spanish = spanishSentence.split("\\s+");
		// System.out.println(spanish[index]);
		String spPos = spanish[index].substring(spanish[index].indexOf('_')+1);

		if (pos.equals("NN") || pos.equals("NNS")) {
			if (!spPos.equals("NOUN")) return true;
		}

		if (pos.equals("JJ")) {
			if (!spPos.equals("ADJ")) return true;
		}

		return false;
	}

	private String fixPos(String pos) {
		if (pos.equals("JJ")) {
			return "NN";
		}else {
			return "JJ";
		}
	}

	private static List<String> cconjunctions = new ArrayList<String>(Arrays.asList("and","nor","but","or","yet"));
	public List<String> processPOS(List<String> taggedSentences, List<String> spanishTaggedSentences, List<ArrayList<Integer>> allPositions) {
		for (String sp: spanishTaggedSentences) System.out.println(sp);
		List<String> toReturn = new ArrayList<String>();
		for(int x = 0; x < taggedSentences.size(); x++) {
			String sent = taggedSentences.get(x);
			// System.out.println(sent);
			String finalStr = "";

			String[] tempTagTokens = sent.split("\\s+");
			ArrayList<Integer> positions = allPositions.get(x);
			String[] tTokens = new String[tempTagTokens.length];
			int prev = -1;
			int run = 0;
			for (int p = 0; p < tTokens.length; p++) {
				int index = positions.get(p);
				if (index == prev) {
					run++;
					tTokens[p - run] = tTokens[p - run] + tempTagTokens[p];
				}else {
					tTokens[p - run] = tempTagTokens[p];
				}
				prev = index;
			}
			String[] tagTokens = new String[tempTagTokens.length - run];
			for (int p = 0; p < tempTagTokens.length - run; p++) {
				tagTokens[p] = tTokens[p];
			}
			for (String s: tempTagTokens) System.out.println(s);
			for (String s: tagTokens) System.out.println(s);


			for(int i = 0; i < tagTokens.length; i++) {
				// System.out.println("English Tagged: " + tagTokens[i]);
				String pos = tagTokens[i].substring(tagTokens[i].indexOf('_')+1);
				// System.out.println(pos);
				String actualWord = tagTokens[i].substring(0, tagTokens[i].indexOf('_'));
				pos = checkPos(pos, i, spanishTaggedSentences.get(x));
				if((!pos.equals("NN") && !pos.equals("NNS")) || (i == tagTokens.length-1)) {
					finalStr += actualWord + " ";
				} else {
					String posNext = tagTokens[i+1].substring(tagTokens[i+1].indexOf('_')+1);
					posNext = checkPos(posNext, i+1, spanishTaggedSentences.get(x));
					if(posNext.equals("JJ")) {
						int numChanged = 1;
						boolean andFound = false;
						List<String> adjectives = new ArrayList<String>();
						while(numChanged+i < tagTokens.length) {
							String posNextNext = tagTokens[i+numChanged].substring(tagTokens[i+numChanged].indexOf('_')+1);
							posNextNext = checkPos(posNextNext, i+numChanged, spanishTaggedSentences.get(x));
							if(!posNextNext.equals("JJ") && !posNextNext.equals("CC"))
								break;
							String nextActualWord = tagTokens[i+numChanged].substring(0, tagTokens[i+numChanged].indexOf('_'));
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

	//ARGUMENTS: corpus file, ngrams file, file to translate
	public static void main(String[] args) {
		Translator t = new Translator();
		String englishTaggerFile = "stanford-postagger-full/models/english-left3words-distsim.tagger";
		String spanishTaggerFile = "stanford-postagger-full/models/spanish-distsim.tagger";

		t.buildDictionary(args[0], args[1]);
		List<String> newSpanishTagged = new ArrayList<String>();
		List<ArrayList<Integer>> positions = new ArrayList<ArrayList<Integer>>();
		List<String> translations = t.translate(args[2], spanishTaggerFile, newSpanishTagged, positions);

		// for (ArrayList<Integer> list: positions) {
		// 	System.out.println(list);
		// }

		System.out.println("We're done translating!");
		System.out.println("Tagging...");
		translations = t.tagger(translations, englishTaggerFile);
		translations = t.processPOS(translations, newSpanishTagged, positions);
		System.out.println("Final translations:");
		for (String translation : translations) {
			translation = translation.replaceAll(" ,", ",");
			translation = translation.replaceAll("-", " ");
			System.out.println(translation);
		}
		System.out.println("--------");
	}

}
