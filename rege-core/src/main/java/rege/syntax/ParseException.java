package rege.syntax;

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when attempting to extract an expression from a failed parse result.
 * 
 * <p>This exception is thrown by {@link ParseResult#orElseThrow()} when the result is a
 * {@link ParseResult.Failure}. It contains all parse errors and the source text.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * try {
 *     Expression expr = RegeReader.parse(input).orElseThrow();
 *     // use expr
 * } catch (ParseException e) {
 *     System.err.println(e.getMessage());
 *     for (ParseError error : e.getErrors()) {
 *         System.err.println(error.formatWithSource(e.getSource()));
 *     }
 * }
 * }</pre>
 */
public class ParseException extends Exception {
    
    private final List<ParseError> errors;
    private final String source;
    
    /**
     * Create a parse exception with errors and source text.
     * 
     * @param errors the list of parse errors (never empty)
     * @param source the source text that failed to parse
     */
    public ParseException(List<ParseError> errors, String source) {
        super(formatMessage(errors, source));
        Objects.requireNonNull(errors, "errors cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("errors cannot be empty");
        }
        this.errors = List.copyOf(errors); // defensive copy
        this.source = source;
    }
    
    /**
     * Get the list of parse errors.
     */
    public List<ParseError> getErrors() {
        return errors;
    }
    
    /**
     * Get the source text that failed to parse.
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Format the exception message with all errors and source context.
     */
    private static String formatMessage(List<ParseError> errors, String source) {
        StringBuilder sb = new StringBuilder();
        sb.append("Parse failed with ").append(errors.size());
        sb.append(errors.size() == 1 ? " error:" : " errors:");
        
        for (int i = 0; i < errors.size(); i++) {
            sb.append("\n\n");
            sb.append(i + 1).append(". ");
            sb.append(errors.get(i).formatWithSource(source));
        }
        
        return sb.toString();
    }
}
