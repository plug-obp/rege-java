package rege.reader.infra;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A peekable character iterator over a string with position tracking.
 * Allows looking ahead at the next character without consuming it,
 * and tracks line, column, and offset for error reporting.
 * 
 * <p>This utility class can be used by parsers to implement one-character
 * lookahead parsing with precise position tracking for error messages.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Peekable input = new Peekable("abc");
 * if (input.hasNext() && input.peek() == 'a') {
 *     Position pos = input.position();
 *     char ch = input.next(); // consume 'a'
 * }
 * }</pre>
 */
public class Peekable implements Iterator<Character> {
    
    private final String input;
    private int offset = 0;
    private int line = 1;
    private int column = 1;
    
    /**
     * Creates a peekable iterator over the given string.
     * 
     * @param input the string to iterate over
     */
    public Peekable(String input) {
        this.input = input;
    }
    
    /**
     * Returns {@code true} if there are more characters to read.
     * 
     * @return true if there are more characters
     */
    @Override
    public boolean hasNext() {
        return offset < input.length();
    }
    
    /**
     * Consumes and returns the next character.
     * Updates position tracking (line, column, offset).
     * 
     * @return the next character
     * @throws NoSuchElementException if there are no more characters
     */
    @Override
    public Character next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters");
        }
        char ch = input.charAt(offset++);
        
        // Track line and column
        if (ch == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        
        return ch;
    }
    
    /**
     * Returns the next character without consuming it.
     * 
     * @return the next character
     * @throws NoSuchElementException if there are no more characters
     */
    public char peek() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters");
        }
        return input.charAt(offset);
    }
    
    /**
     * Get the current position in the input.
     * 
     * @return the current position (line, column, offset)
     */
    public Position position() {
        return new Position(line, column, offset);
    }
    
    /**
     * Create a range from the given start position to the current position.
     * Useful for marking the span of a parsed token or expression.
     * 
     * @param start the start position
     * @return a range from start to current position
     */
    public Range rangeFrom(Position start) {
        return new Range(start, position());
    }
    
    /**
     * Create a zero-width range at the current position.
     * Useful for marking an error at a specific location.
     * 
     * @return a range at the current position
     */
    public Range rangeHere() {
        return Range.at(position());
    }
    
    /**
     * Get the source string being parsed.
     * 
     * @return the source string
     */
    public String source() {
        return input;
    }
}
