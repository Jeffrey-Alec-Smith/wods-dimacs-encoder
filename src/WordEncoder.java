import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WordEncoder {

    /**
     * Offset to subtract from the ordinal values of letters such that letters 'a' = 0, 'b' = 1, ..., up to 'z' = 25
     * This will ensure we can encode every letter within 5 bits
     */
    private static final int LETTER_ORDINAL_OFFSET = 96;
    private static final int BITS_PER_LETTER = 5;
    private static final int WORD_MASK = 0b00000010_00000000_00000000_00000000;
    private static final Map<String, Integer> encodingMapping = new HashMap<>();

    private final List<String> wordList;
    private static final String mappingFileName = "mapping.txt";

    public WordEncoder(List<String> words) {
        this.wordList = words;
    }

    public int byteEncodeWord(String word) {
        word = word.toLowerCase();
        var encoding = WORD_MASK;
        for (var index = 0; index < 5; index++) {
            var characterValue = word.charAt(index) - LETTER_ORDINAL_OFFSET;
            var shiftAmount = (4 - index) * BITS_PER_LETTER;
            encoding = encoding | (characterValue << shiftAmount);
        }
        encodingMapping.put(word, encoding);
        return encoding;
    }

    public int encodeWord(String word) {
        var encoding = wordList.indexOf(word) + 26;
        encodingMapping.put(word, encoding);
        return encoding;
    }

    public static String convertIntToBinaryString(int value, char separator, int interval, int offset) {
        var sb = new StringBuilder();
        var counter = -offset;
        var indexer = 1 << 31;
        for (int i = 0; i < 32; i++) {
            // Append separator if the interval has been reached and the interval won't be the first character
            if (counter >= interval && i > 0) {
                sb.append(separator);
                counter = 0;
            }
            counter++;
            var active = value & indexer;
            indexer = indexer >>> 1;
            sb.append(active > 0 ? 1 : 0);
        }
        return sb.toString();
    }

    public void writeMappingFile() {
        try(var pw = new PrintWriter(new BufferedWriter(new FileWriter(mappingFileName)))) {
            var sb = new StringBuilder();
            for (String k : encodingMapping.keySet().stream().sorted(String::compareTo).toList()) {
                sb.append(k).append(": ").append(encodingMapping.get(k)).append("\n");
            }
            pw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
