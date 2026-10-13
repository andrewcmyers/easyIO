package easyIO;

/** The character sequences that a scanner accepts as line terminators. */
public enum LineTerminators {
    /** Only "\n" terminates a line. */
    LF,
    /** "\n" and "\r\n" terminate a line. */
    LF_CRLF,
    /** "\n", "\r\n", and "\r" terminate a line. A "\r" immediately followed by
     *  "\n" is a single line terminator. */
    LF_CRLF_CR
}
