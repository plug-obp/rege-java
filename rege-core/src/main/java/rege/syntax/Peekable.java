package rege.syntax;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A peekable character iterator over a string.
 * Allows looking ahead at the next character without consuming it.
 * 
 * <p>This utility class is used by both {@link RegeReader} and {@link RegeReaderLeft}
 * to parse regular expression syntax with one-character lookahead.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Peekable input = new Peekable("abc");
 * if (input.hasNext() && input.peek() == 'a') {
 *     char ch = input.next(); // consume 'a'
 * }
 * }</pre>
 */
public class Peekable implements Iterator<Character> {
    
    private final String input;
    private int position = 0;
    
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
        return position < input.length();
    }
    
    /**
     * Consumes and returns the next character.
     * 
     * @return the next character
     * @throws NoSuchElementException if there are no more characters
     */
    @Override
    public Character next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters");
        }
        return input.charAt(position++);
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
        return input.charAt(position);
    }
}
