package rege.reader.infra;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a parse error with optional position information and severity.
 * 
 * <p>Located errors map to LSP (Language Server Protocol) Diagnostic values.
 * Unlocated errors must be reported through a channel that does not need a range.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Range range = Range.at(new Position(0, 4, 4));
 * ParseError error = new ParseError(
 *     range,
 *     "Unexpected character '}'",
 *     ParseError.Severity.ERROR,
 *     "unexpected-char"
 * );
 * }</pre>
 * 
 * @param range the source range where the error occurred, or null if unavailable
 * @param message the error message
 * @param severity the severity level of the error
 * @param code optional error code for categorization
 */
public record ParseError(
    Range range,
    String message,
    Severity severity,
    Optional<String> code
) {
    
    /**
     * Error severity levels, compatible with LSP DiagnosticSeverity.
     */
    public enum Severity {
        /** Indicates an error that prevents parsing. */
        ERROR(1),
        /** Indicates a warning that doesn't prevent parsing. */
        WARNING(2),
        /** Indicates an informational message. */
        INFO(3),
        /** Indicates a hint or suggestion. */
        HINT(4);
        
        private final int lspValue;
        
        Severity(int lspValue) {
            this.lspValue = lspValue;
        }
        
        /**
         * Get the LSP DiagnosticSeverity value.
         * @return the LSP severity value (1-4)
         */
        public int getLspValue() {
            return lspValue;
        }
    }
    
    /**
     * Compact constructor that validates all fields.
     * Ensures message, severity, and code are non-null and message is not blank.
     * An absent range is preserved; no synthetic location is substituted.
     */
    public ParseError {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(code, "code cannot be null");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
    }
    
    /**
     * Create an ERROR-level parse error without an error code.
     * @param range the source range where the error occurred, or null if unavailable
     * @param message the error message
     */
    public ParseError(Range range, String message) {
        this(range, message, Severity.ERROR, Optional.empty());
    }
    
    /**
     * Create a parse error with a severity but no error code.
     * @param range the source range where the error occurred, or null if unavailable
     * @param message the error message
     * @param severity the severity level
     */
    public ParseError(Range range, String message, Severity severity) {
        this(range, message, severity, Optional.empty());
    }
    
    /**
     * Create an ERROR-level parse error with an error code.
     * @param range the source range where the error occurred, or null if unavailable
     * @param message the error message
     * @param code the error code for categorization
     */
    public ParseError(Range range, String message, String code) {
        this(range, message, Severity.ERROR, Optional.of(code));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity.name().toLowerCase());
        if (range != null) {
            sb.append(" at ").append(displayPosition(range.start()));
            if (!range.start().equals(range.end())) {
                sb.append("-").append(displayPosition(range.end()));
            }
        }
        sb.append(": ");
        sb.append(message);
        code.ifPresent(c -> sb.append(" [").append(c).append("]"));
        return sb.toString();
    }

    private static String displayPosition(Position position) {
        return ((long) position.line() + 1) + ":" + ((long) position.column() + 1);
    }
    
    /**
     * Format this error with the source text for better context.
     * Shows the line with an indicator pointing to the error location.
     * Displayed line and column numbers are one-based; stored coordinates are
     * always zero-based. Position and Range string representations stay zero-based.
     * 
     * @param source the source text that was being parsed, or null if unavailable
     * @return a formatted error message with source context
     */
    public String formatWithSource(String source) {
        if (range == null || source == null) {
            return toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        
        // Extract the line containing the error
        SourceText text = new SourceText(source);
        int lineIndex = range.start().line();
        
        if (lineIndex < text.lineCount()) {
            String line = source.substring(text.lineStartOffset(lineIndex), text.lineEndOffset(lineIndex));
            sb.append("  ").append(line).append("\n");
            
            // Add indicator
            sb.append("  ");
            int col = Math.min(range.start().column(), line.length());
            sb.append(" ".repeat(Math.max(0, col)));
            
            int endColumn = range.end().line() == lineIndex
                ? Math.min(range.end().column(), line.length()) : line.length();
            int errorLength = Math.max(1, endColumn - col);
            sb.append("^".repeat(errorLength));
        }
        
        return sb.toString();
    }
}
