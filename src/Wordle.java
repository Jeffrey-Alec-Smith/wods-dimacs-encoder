import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Game Master for a game of Wordle, handles all the logic required to play the game.
 */
public class Wordle {

    private static final String DICTIONARY_DIRECTORY = "./resources/dictionaries/";
    private static final String SOLUTIONS_DICTIONARY_FILENAME = "solutions.txt";
    private static final String VALID_DICTIONARY_FILENAME = "valid.txt";
    private static final int LETTER_ORDINAL_OFFSET = 96;
    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz";
    private List<String> solutionSet; // Set of Words Wordle chooses from for possible solutions
    private List<String> validSet; // Set of Words for which Wordle will give a response
    //private PrintWriter pw;
    private final StringBuilder sb = new StringBuilder();
    private List<String>[] letterGroups;
    private List<Integer>[] letterGroupEncodings;
    private final Set<Integer> literalsSet = new HashSet<>();
    private final AtomicInteger clauses = new AtomicInteger();
    private final WordEncoder encoder;

    public Wordle(String outname) throws IOException {
        initializeSolutionSet();
        initializeValidSet();

        solutionSet = validSet.stream()
                .filter(s -> !containsDuplicateCharacter(s))
                .sorted()
                .collect(Collectors.toList());

        solutionSet = removeAnagrams(solutionSet);

        encoder = new WordEncoder(solutionSet);
        groupWords(solutionSet);

         // change if needed
        for (List<Integer> group : letterGroupEncodings) {
            oneHotEncode(group, false);
        }

        // Each word implies its letters
        solutionSet
                .stream()
                .sorted()
                .forEachOrdered(this::implyLetters);

        // exactly 25 out of 26 letters are contained in the set <=> 1 out of 26 letters is NOT contained in the set
        var range = IntStream.rangeClosed(1, 26).boxed().collect(Collectors.toList());
        oneHotEncode(range, true);

        // Each letter in the set must be implied by at least 1 word
        for (int i = 0; i < letterGroupEncodings.length; i++) {
            var group = letterGroupEncodings[i];
            sb.append("\n").append(-(i + 1));
            for (var encoding : group) {
                sb.append(" ").append(encoding);
            }
            sb.append(" 0");
            clauses.getAndIncrement();
        }
        // Add the header
        sb.insert(0, "p cnf " + literalsSet.size() + " " + clauses.get());

        encoder.writeMappingFile();

        System.out.println("Solution Set size: " + solutionSet.size());
        System.out.println("Number of literals used: " + literalsSet.size());
        System.out.println("Number of clauses used: " + clauses.get());
        System.out.println("Max var value: " + literalsSet.stream().max(Integer::compareTo));


        try (var pw = new PrintWriter(new BufferedWriter(new FileWriter(outname)))) {
            pw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> removeAnagrams(List<String> words) {
        var anagramless = new ArrayList<String>();
        for (String word : words) {
            var hasAnagram = false;
            for (String other : anagramless) {
                if (isAnagram(word, other)) {
                    hasAnagram = true;
                    System.out.println(word + " IS ANAGRAM OF " + other);
                    break;
                }
            }
            if (!hasAnagram) anagramless.add(word);
        }
        return anagramless;
    }

    private boolean isAnagram(String s1, String s2) {
        for (var c : s1.toCharArray()) {
            if (s2.indexOf(c) < 0) return false;
        }
        return true;
    }

    private void implyLetters(String word) {
        var encoding = encoder.encodeWord(word);
        //sb.append(word).append("\n");
        for (char c : word.toCharArray()) {
            var letterEncoding = c - LETTER_ORDINAL_OFFSET;
            sb.append("\n").append(-encoding).append(" ").append(letterEncoding).append(" 0");
            literalsSet.add(Math.abs(encoding));
            literalsSet.add(Math.abs(letterEncoding));
            clauses.getAndIncrement();
        }
    }

    private void groupWords(Collection<String> words) {
        letterGroups = new List[CHARACTER_SET.length()]; // One list for each letter of the alphabet
        // Group Strings
        //System.out.println("WORD GROUPS");
        for (int i = 0; i < CHARACTER_SET.length(); i++) {
            var currentChar = CHARACTER_SET.charAt(i);
            letterGroups[i] = new ArrayList<>();
            for (var word : words) {
                if (word.indexOf(currentChar) >= 0) {
                    letterGroups[i].add(word);
                }
            }
            //System.out.println(letterGroups[i]);
        }
        // Convert to int encodings
        //System.out.println("WORD GROUP ENCODINGS");
        letterGroupEncodings = new List[CHARACTER_SET.length()];
        for (int i = 0; i < CHARACTER_SET.length(); i++) {
            var groupEncoding = new ArrayList<Integer>();
            var group = letterGroups[i];
            for (String word : group) {
                groupEncoding.add(encoder.encodeWord(word));
            }
            //System.out.println(groupEncoding);
            letterGroupEncodings[i] = groupEncoding;
        }

    }

    private void oneHotEncode(List<Integer> literals, boolean oneCold) {
        literals.stream()
                .forEachOrdered((antecedent) -> literals.stream()
                        //.filter((consquent) -> !consquent.equals(antecedent))
                        .forEachOrdered((consequent) -> {
                            if (consequent < antecedent) {
                                sb.append("\n")
                                        .append(oneCold ? antecedent : -antecedent)
                                        .append(" ")
                                        .append(oneCold ? consequent : -consequent)
                                        .append(" 0");
                                literalsSet.add(Math.abs(antecedent));
                                literalsSet.add(Math.abs(consequent));
                                clauses.getAndIncrement();
                            }
                        }));
    }

    private void initializeValidSet() {
        validSet = new ArrayList<>();
        validSet.addAll(solutionSet);
        var validDictionaryFilePath = DICTIONARY_DIRECTORY + VALID_DICTIONARY_FILENAME;
        try (var br = new BufferedReader(new FileReader(validDictionaryFilePath))) {
            br.lines().forEach(s -> validSet.add(s));
        } catch (Exception E) {
            E.printStackTrace();
            throw new Error("Unable to initialize valid set dictionary.");
        }
        System.out.println("Valid Dict Size: " + validSet.size());
    }

    private void initializeSolutionSet() {
        solutionSet = new ArrayList<>();
        var solutionsDictionaryFilePath = DICTIONARY_DIRECTORY + SOLUTIONS_DICTIONARY_FILENAME;
        try (var br = new BufferedReader(new FileReader(solutionsDictionaryFilePath))) {
            br.lines().forEach(s -> solutionSet.add(s));
        } catch (Exception E) {
            E.printStackTrace();
            throw new Error("Unable to initialize solution set dictionary.");
        }
        System.out.println("Solution Dict Size: " + solutionSet.size());
    }

    /**
     *
     * @param word String to be checked
     * @return whether word contains any duplicate characters
     */
    private static boolean containsDuplicateCharacter(String word) {
        for (int i = 0; i < word.length() - 1; i++) {
            for (int k = i + 1; k < word.length(); k++) {
                if (word.charAt(i) == word.charAt(k)) return true;
            }
        }
        return false;
    }

}
