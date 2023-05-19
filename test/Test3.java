import easyIO.EOF;
import easyIO.Scanner;
import easyIO.UnexpectedInput;

import java.io.FileNotFoundException;

public class Test3 {
    public static void main(String[] args) throws FileNotFoundException {

        Scanner s = new Scanner(args[0]);
        System.out.println("Reading chars: ");
        try {
            while (s.hasNext()) {
                System.out.printf("Line %d, column %d: ", s.lineNo(), s.column());
                char c = s.next();
                System.out.printf("0x%x: %c\n", (int)c, Character.isWhitespace(c) ? ' ' : c);
            }
        } catch (EOF e) {
            System.out.println("Caught EOF!?");
        }
        s = new Scanner(args[0]);
        System.out.println("Reading code points: ");
        try {
            while (s.hasNext()) {
                System.out.printf("Line %d, column %d: ", s.lineNo(), s.column());
                int c = s.nextCodePoint();
                char[] chars = Character.toChars(c);
                String str = new String(chars);
                System.out.printf("0x%x: %s\n", c, str);
            }
        } catch (EOF e) {
            System.out.println("Caught EOF!?");
        }
    }
}
