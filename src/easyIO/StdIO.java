package easyIO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/** Easy access to standard input and standard output, without fussing with exceptions. One
    easy way to use this is to make your class a subclass of {@code StdIO}. Or you can use a static
    import:
    <pre>
    import static easyIO.StdIO.*;
    ...
    </pre>
    
    */
public class StdIO {
    /** Standard input, as a {@code BufferedReader} */
	final static public BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    /** Standard input, as a {@code PrintWriter} */
	final static public PrintWriter stdout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
	
	protected StdIO() {}

    /** Output a string to standard output.
     *  @param s the string to print
     */
	public static void print(String s) {
		stdout.print(s);
	}
    /** Output a string to standard output, followed by a newline.
     *  @param s the string to print
     */
	public static void println(String s) {
		stdout.println(s);
		stdout.flush();
	}
    /** Output a newline to standard output. */
	public static void println() {
		stdout.println(); stdout.flush();
	}
    /** Print a char to standard output.
     *  @param c the char to print
     */
	public static void print(char c) {
		stdout.print(c);
	}
    /** Print an int to standard output.
     *  @param x the value to print
     */
	public static void print(int x) {
		stdout.print(x);
	}
    /** Print an int to standard output, followed by a newline.
     *  @param x the value to print
     */
	public static void println(int x) {
		stdout.println(x);
	}
    /** Read a line from standard input.
     *  Requires that there is a line to read.
     *  @return the string read
     */
	public static String readln() {
		try {
			return stdin.readLine();
		} catch (IOException e) {
			throw new Error("IO exception on standard input");
		}
	}
}
