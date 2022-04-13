import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            var wordle = new Wordle("out.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
