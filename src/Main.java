public class Main {

    public static void main(String[] args) {
        var enc = new WordEncoder();
        var val = enc.encodeWord("ABCDE");
        System.out.println(val);
        System.out.println(enc.convertIntToBinaryString(val, '_', 5, 2));
        var wordle = new Wordle();
    }
}
