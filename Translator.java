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
			System.exit(1);
		}
	}

	public List<String> translate(String fileStr) {
		List<String> translations = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(fileStr)));
			while(true) {
				String line = br.readLine();
				if (line == null) break;
				line = line.toLowerCase();
				translations.add(translateLineSpanEng(line));
			}
		} catch(IOException e) {
			System.exit(1);
		}
		return translations;
	}

	public String translateLineSpanEng(String line) {
		List<String> finalTranslation = new ArrayList<String>();
		//remove punctuation
		String[] tokens = line.replaceAll("[.]", "").split("\\s+");
		for(int i = 0; i < tokens.length; i++) {
			boolean hasComma = false;
			String mostLikelyTrans = "??";
			double value = 0.0;
			if(tokens[i].contains(",")) {
				hasComma = true;
				tokens[i] = tokens[i].substring(0, tokens[i].length()-1);
			}
			// System.out.println(dictionaryMap);
			// System.out.println("word to lookup in dict: " + tokens[i]);
			// System.out.println("---->" + dictionaryMap.get(tokens[i]));
			// System.out.println(dictionaryMap.get(tokens[i]).get(0));
			if(dictionaryMap.get(tokens[i]) != null) {
				// mostLikelyTrans = dictionaryMap.get(tokens[i]).get(0);
				for(String transWord : dictionaryMap.get(tokens[i])) {
					// System.out.println(transWord);
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
				// System.out.println(tokens[i]);
				if((tokens[i].equals("del")) || (tokens[i].equals("al"))) {
					String firstWord = tokens[i].substring(0,tokens[i].indexOf('l'));
					String secondWord = "e" + tokens[i].substring(tokens[i].indexOf('l'));
					mostLikelyTrans = dictionaryMap.get(firstWord).get(0) + " " + dictionaryMap.get(secondWord).get(0);
				} else
					mostLikelyTrans = "??";
			}
			// if(!mostLikelyTrans.equals("")) {
			finalTranslation.add(mostLikelyTrans);
			if(hasComma) {
				finalTranslation.add(",");
			}
			// } else {
			// 	finalTranslation += "!!! ";
			// }
		}
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
    		System.out.println(taggedSentence);
    		toReturn.add(taggedSentence);
    	}
    	return toReturn;
	}

	private static List<String> cconjunctions = new ArrayList<String>(Arrays.asList("and","nor","but","or","yet"));

	public List<String> processPOS(List<String> taggedSentences) {
		List<String> toReturn = new ArrayList<String>();
		for(String sent : taggedSentences) {
			String finalStr = "";
			String[] tagTokens = sent.split("\\s+");
			for(int i = 0; i < tagTokens.length; i++) {
				String pos = tagTokens[i].substring(tagTokens[i].indexOf('_')+1);
				String actualWord = tagTokens[i].substring(0, tagTokens[i].indexOf('_'));
				if((!pos.equals("NN") && !pos.equals("NNS")) || (i == tagTokens.length-1)) {
					finalStr += actualWord + " ";
				} else {
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
		String taggerFile = "stanford-postagger-full/models/english-left3words-distsim.tagger";

		t.buildDictionary(args[0], args[1]);
		List<String> translations = t.translate(args[2]);

		System.out.println("We're done translating!");
		System.out.println("Tagging...");
		translations = t.tagger(translations, taggerFile);

		translations = t.processPOS(translations);
		System.out.println("Final translations:");
		for (String translation : translations) {
			translation = translation.replaceAll(" ,", ",");
			System.out.println(translation);
		}
		System.out.println("--------");
	}

}
