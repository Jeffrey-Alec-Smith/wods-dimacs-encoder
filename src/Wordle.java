import java.beans.Encoder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A Game Master for a game of Wordle, handles all the logic required to play the game.
 */
public class Wordle {

    private static final String DICTIONARY_DIRECTORY = "./resources/dictionaries/";
    private static final String SOLUTIONS_DICTIONARY_FILENAME = "solutions.txt";
    private static final String VALID_DICTIONARY_FILENAME = "valid.txt";

    private static final int ALPHABET_INDEX = 97;

    /**
     * The set of possible solutions in Wordle.
     * It is a subset of the dictionary of valid guesses
     * and is not intended to be public knowledge to a player.
     */
    List<String> solutionSet;

    /**
     * Complete dictionary of valid guesses.
     */
    List<String> validSet;

    public Wordle() {
        initializeSolutionSet();
        initializeValidSet();
        System.out.println("Wordle initialized");
        System.out.println(solutionSet.size() + " words in solution set");
        System.out.println(validSet.size() + " words in valid set");
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

    private int[] encodeWords(List<String> words) {
        var encodings = new int[words.size()];
        var encoder = new WordEncoder();
        for (int i = 0; i < encodings.length; i++) {
            encodings[i] = encoder.encodeWord(words.get(i));
        }
        return encodings;
    }

    /**
     *
     * @param word String to be checked
     * @return whether word contains any duplicate characters
     */
    private boolean contains_duplicate_character(String word) {
        for (int i = 0; i < word.length() - 1; i++) {
            for (int k = i + 1; k < word.length(); k++) {
                if (word.charAt(i) == word.charAt(k)) return true;
            }
        }
        return false;
    }

}
