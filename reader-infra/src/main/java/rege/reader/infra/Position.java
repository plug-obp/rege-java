package rege.reader.infra;

import java.util.Objects;

/**
 * Represents a position in source text.
 * 
 * <p>Position uses 1-based line and column numbers (standard for editors and LSP).
 * The offset is 0-based for direct string indexing.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Position pos = new Position(1, 5, 4);  // Line 1, column 5, offset 4
 * Position start = Position.start();      // (1, 1, 0)
 * }</pre>
 * 
 * @param line 1-based line number
 * @param column 1-based column number (UTF-16 code units)
 * @param offset 0-based character offset from start of input
 */
public record Position(int line, int column, int offset) implements Comparable<Position> {
    
    /**
     * Compact constructor that validates line, column, and offset values.
     */
    public Position {
        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got: " + line);
        }
        if (column < 1) {
            throw new IllegalArgumentException("column must be >= 1, got: " + column);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
    }
    
    /**
     * Create a position at the start of input (line 1, column 1, offset 0).
     * @return a position representing the start of input
     */
    public static Position start() {
        return new Position(1, 1, 0);
    }
    
    @Override
    public int compareTo(Position other) {
        return Integer.compare(this.offset, other.offset);
    }
    
    @Override
    public String toString() {
        return line + ":" + column;
    }
}
