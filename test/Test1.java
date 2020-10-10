import easyIO.BacktrackScanner;

import java.io.InputStreamReader;

class Test1 {
    public static void main(String[] args) {
        BacktrackScanner sc = new BacktrackScanner();
        sc.appendSource(new InputStreamReader(System.in), "System.in");

        try {
            sc.mark();
            System.out.println("after mark sc = " + sc);
            char a1 = sc.next();
            System.out.println("a1 = " + a1 + ", sc = " + sc);
            char a2 = sc.next();
            System.out.println("a2 = " + a2 + ", sc = " + sc);
            sc.abort();
            System.out.println("after abort sc = " + sc);
            char a3 = sc.next();
            System.out.println("a3 = " + a3 + ", sc = " + sc);
            sc.mark();
            System.out.println("after second mark sc = " + sc);
            char a4 = sc.next();
            System.out.println("a4 = " + a4 + ", sc = " + sc);
            char a5 = sc.next();
            System.out.println("a5 = " + a5 + ", sc = " + sc);
            char a6 = sc.next();
            System.out.println("a6 = " + a6 + ", sc = " + sc);
            sc.accept();
            System.out.println("after accept sc = " + sc);
        } catch (easyIO.EOF e) {
            System.out.println("Oops, saw EOF");
            System.exit(1);
        }
    }

}
