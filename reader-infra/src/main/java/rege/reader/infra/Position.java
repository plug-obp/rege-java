package rege.reader.infra;

/**
 * Represents a position in source text.
 * 
 * <p>Line, column, and offset are zero-based. Column and offset count UTF-16
 * code units, matching Java string indexing and LSP's default position encoding.
 * Use {@link SourceText} to derive consistent coordinates from original text.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Position pos = new Position(0, 4, 4);  // First line, fifth UTF-16 code unit
 * Position start = Position.start();      // (0, 0, 0)
 * }</pre>
 * 
 * @param line zero-based line number
 * @param column zero-based column (UTF-16 code units)
 * @param offset zero-based UTF-16 offset from start of input
 */
public record Position(int line, int column, int offset) implements Comparable<Position> {
    
    /**
     * Compact constructor that validates line, column, and offset values.
     */
    public Position {
        if (line < 0) {
            throw new IllegalArgumentException("line must be >= 0, got: " + line);
        }
        if (column < 0) {
            throw new IllegalArgumentException("column must be >= 0, got: " + column);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
    }
    
    /**
     * Create a position at the start of input (line 0, column 0, offset 0).
     * @return a position representing the start of input
     */
    public static Position start() {
        return new Position(0, 0, 0);
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
