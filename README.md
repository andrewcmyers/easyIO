# easyIO
A replacement for the Java Scanner class. Originally developed for Cornell CS 2112.

Unlike the standard Scanner class, it supports backtracking, by maintaining a stack of marks.

Its regular expression support is based on Brzozowski derivatives and, because it does *not*
rely on backtracking, does not suffer the exponential blowup on pathological cases that Java's
regular expression support does. It also supports capture expressions.

Its behavior is a little different and more explicit than Java's, because it's intended for real parsing tasks.
In particular, it doesn't automatically skip over whitespace, and nextLine() returns the whole line including terminators.

## Documentation

https://andrewcmyers.github.io/easyIO/
