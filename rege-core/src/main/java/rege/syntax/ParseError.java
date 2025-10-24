package rege.syntax;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a parse error with position information and severity.
 * 
 * <p>This structure is compatible with LSP (Language Server Protocol) Diagnostic type.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Range range = Range.at(new Position(1, 5, 4));
 * ParseError error = new ParseError(
 *     range,
 *     "Unexpected character '}'",
 *     ParseError.Severity.ERROR,
 *     "unexpected-char"
 * );
 * }</pre>
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
        
        /** Get the LSP DiagnosticSeverity value. */
        public int getLspValue() {
            return lspValue;
        }
    }
    
    public ParseError {
        Objects.requireNonNull(range, "range cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(code, "code cannot be null");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
    }
    
    /**
     * Create an ERROR-level parse error without an error code.
     */
    public ParseError(Range range, String message) {
        this(range, message, Severity.ERROR, Optional.empty());
    }
    
    /**
     * Create a parse error with a severity but no error code.
     */
    public ParseError(Range range, String message, Severity severity) {
        this(range, message, severity, Optional.empty());
    }
    
    /**
     * Create an ERROR-level parse error with an error code.
     */
    public ParseError(Range range, String message, String code) {
        this(range, message, Severity.ERROR, Optional.of(code));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity.name().toLowerCase());
        sb.append(" at ").append(range).append(": ");
        sb.append(message);
        code.ifPresent(c -> sb.append(" [").append(c).append("]"));
        return sb.toString();
    }
    
    /**
     * Format this error with the source text for better context.
     * Shows the line with an indicator pointing to the error location.
     * 
     * @param source the source text that was being parsed
     * @return a formatted error message with source context
     */
    public String formatWithSource(String source) {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        
        // Extract the line containing the error
        String[] lines = source.split("\n", -1);
        int lineIndex = range.start().line() - 1;
        
        if (lineIndex >= 0 && lineIndex < lines.length) {
            String line = lines[lineIndex];
            sb.append("  ").append(line).append("\n");
            
            // Add indicator
            sb.append("  ");
            int col = range.start().column() - 1;
            sb.append(" ".repeat(Math.max(0, col)));
            
            int errorLength = Math.max(1, range.length());
            sb.append("^".repeat(errorLength));
        }
        
        return sb.toString();
    }
}
