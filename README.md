# assignment6-mt
CS124 Final Project (PA6) Spanish->English Direct Translator
Created by Carlos Girod, Kiana Hui, Alec Powell & Hailey Spelman

USAGE
------------------
To compile the Java code:
javac -cp "stanford-postagger-3.5.1.jar" Translator.java

To run the translator:
java -cp "stanford-postagger-3.5.1.jar":. Translator dictionary.txt ngrams.txt input_dev.txt 

*Make sure you have the Stanford POS tagger downloaded and the JAR file in the same directory as the .java file. English/spanish .props files (see code) should be located in directory /stanford-postagger-full/models/
