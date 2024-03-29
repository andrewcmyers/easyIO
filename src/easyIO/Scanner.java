package easyIO;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

/** A {@code BacktrackScanner} extended with convenience methods for
 * parsing common things like integers and identifiers.
 *    Compared to a {@code java.util.Scanner}, it has the ability to peek()
 *  at the next character on the input and the ability to read its input from
 *  multiple Readers, which may be added on the fly. Beyond this, it can also
 *  do arbitary lookahead and rollback of the input stream using the
 *  mark(), accept(), and abort() methods from {@code BacktrackScanner}.
 *    Generally, all methods parse input starting at the current
 *  position, and throw {@code UnexpectedInput} if the expected input is not
 *  found. If an exception is thrown, the current input position is left
 *  unchanged.
 */
public class Scanner extends BacktrackScanner {
		
    /** Create a scanner that has no input source (yet). */
	public Scanner() {
	}

	public Scanner(Source s) {
		super(s);
	}
	
	/** Create a scanner that reads from input source {@code r},
     *  calling the source {@code name}.
     *  @param r The input source
     *  @param name The name of the input source
     */
	public Scanner(Reader r, String name) {
		super();
		includeSource(r, name);
	}
	
	/** Create a scanner that reads from file named {@code filename}. To read
	 *  directly from a string, use a {@code StringReader}.
	 *  @see java.io.StringReader
	 */
	public Scanner(String filename) throws FileNotFoundException {
		super();
		includeSource(new BufferedReader(new FileReader(filename)), filename);
	}

	/** Scan past any whitespace. */
	public void whitespace() {
		while (hasNext() && Character.isWhitespace(peek()))
			advance();
	}

    /** Scan past the specified string. 
     *  @throws UnexpectedInput if anything other than the expected string
     *       is encountered, leaving the scanner at the position where an
     *       unexpected character is reached.
     */
    public void consume(String s) throws UnexpectedInput {
        for (int i = 0; i < s.length(); i++) {
            if (!hasNext()) throw uinp;
            int c = peek();
            if ((char) c != s.charAt(i)) throw uinp;
            advance();
        }
    }

	/** Scan past and return the next character if it is a digit.*/
	public char nextDigit() throws UnexpectedInput {
		int c = peek();
		if (Character.isDigit((char) c)) {
			advance();
			return (char) c;
		}
		throw uinp;
	}

	/** Scan past all digits at the current posn, if any. */
	public void optDigits() {
		try {
			while (true) {
				if (!hasNext() || !Character.isDigit(peek())) { break; }
				nextDigit();
			}
		} catch (UnexpectedInput e) {
				assert false;
			}
		
	}

	/** Scan past an integer constant. 
	 * @throws UnexpectedInput
	 *             if the characters at the current position are not a
	 *             integer literal.
	 */
	public void integer() throws UnexpectedInput {
		try {
			mark();
			if (peek() == '-') { advance(); }
			nextDigit();
			optDigits();
			accept();
			return;
		} catch (UnexpectedInput uinp) {
			abort();
			throw uinp;
		}
	}

	/**
	 * Scan a floating-point number.
	 * 
	 * @throws UnexpectedInput
	 *             if the characters at the current position are not a
	 *             floating-point literal.
	 */
	public void floatingPoint() throws UnexpectedInput {
		boolean before_point = false, after_point = false;
		int m = nmarks;
		mark();
		try {
			int n = peek();
			if (n == '-') advance();
			int c = peek();
			if (Character.isDigit(c)) {
				digits();
				before_point = true;
			}
			if (peek() == '.') {
				advance();
				if (Character.isDigit(peek())) after_point = true;
				if (before_point)
					optDigits();
				else
					digits();
			}
			if (!before_point && !after_point) {
				throw uinp;
			}
			int d = peek();
			if (d == 'e' || d == 'E') {
				advance();
				digits();
			}
			accept();
			assert nmarks == m;
		} catch (UnexpectedInput e) {
			abort();
			assert nmarks == m;
			throw uinp;
		}
	}

	/** Scan one or more digits.
	 * @throws UnexpectedInput if the next character is not a digit. */
	public void digits() throws UnexpectedInput {
		mark();
		try {
			nextDigit();
			optDigits();
			accept();
			return;
		} catch (UnexpectedInput uinp) {
			abort();
			return;
		}
	}

	/** Scan an identifier ala Java.
        @throws UnexpectedInput if the next characters are not an identifier. */
	public void identifier() throws UnexpectedInput {
		int first = peek();
		if (!Character.isAlphabetic(first) && first != '_')
			throw uinp;
		advance();
		while (Character.isAlphabetic(peek()) || Character.isDigit(peek()) || peek() == '_') {
			advance();
		}
	}

	/**
	 * Scan past an integer constant and return its value.
	 * 
	 * @throws UnexpectedInput
	 *             if the characters following the current position are not an integer
	 *             literal.
	 */
	public int nextInt() throws UnexpectedInput {
		mark();
		try {
			integer();
			int r = Integer.parseInt(getToken());
			accept();
			return r;
		} catch (UnexpectedInput e) {
			abort();
			throw e;
		} catch (NumberFormatException e) {
			abort();
			throw e;
		}
	}

	/**
     * Scan past a floating-point constant and return its value as a
     * {@code double}
	 * 
	 * @throws UnexpectedInput
     *             if the characters following the current position are not a
     *             legal representation of a double literal.
	 */
	public double nextDouble() throws UnexpectedInput {
		mark();
		try {
			floatingPoint();
			double r = Double.parseDouble(getToken());
			accept();
			return r;
		} catch (UnexpectedInput e) {
			abort();
			throw e;
		} catch (NumberFormatException e) {
			abort();
			throw e;
		}
	}

	/**
	 * Scan past and return all text from the current position to the next
	 * newline character.
	 * 
	 * @throws UnexpectedInput
	 *             if there is no terminated line after the current position.
	 */
	public String nextLine() throws UnexpectedInput {
		mark();
		while (hasNext()) {
			try {
				int c = nextCodePoint();
				if (c == '\n') break;
			} catch (EOF e) {
				assert false;
				break;
			}
		}
		String ret = getToken();
		accept();
		return ret;
	}
	/** Scan past and return an identifier.
	 *  @throws UnexpectedInput
	 *             if the characters at the current position are not a
	 *             legal Java identifier.
	 */
	public String nextIdentifier() throws UnexpectedInput {
		mark();
		try {
			identifier();
			String r = getToken();
			accept();
			return r;
		} catch (UnexpectedInput e) {
			abort();
			throw e;
		}
	}
	/** Scan past a optional carriage return character and a newline character.
	 * 
	 * @throws UnexpectedInput if the next characters are neither "\r\n" nor "\n".
	 */
	public void newline() throws UnexpectedInput {
		mark();
		if (peek() == '\r') advance();
		if (peek() == '\n') {
			advance();
			accept();
			return;
		} else {
			abort();
			throw uinp;
		}
	}

	/**
	 * Scan to the end of the current line or to the end of the input, whichever
	 * is first.
	 */
	public void eol() {
		while (true) {
			try {
				int c = nextCodePoint();
				if (c == '\n') return;
			} catch (EOF e) {
				return;
			}
		}
	}
	
	/**
	 * Scan past any whitespace up to the end of the line, the end of the file,
	 * or the next non-whitespace character.
	 */
	public void trailingWhitespace() {
		while (true) {
			int c = peek();
			switch (c) {
			 case -1:
			 case '\n':
			 case '\f': // treat form feed like newline
				return;
			}
			if (!Character.isWhitespace(c)) return;
			advance();
		}
	}

}
