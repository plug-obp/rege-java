package rege.syntax;

import java.util.Objects;

/**
 * Represents a range in source text, from a start position to an end position.
 * 
 * <p>This is compatible with LSP (Language Server Protocol) Range type.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Position start = new Position(1, 1, 0);
 * Position end = new Position(1, 5, 4);
 * Range range = new Range(start, end);
 * 
 * // Zero-width range at a position
 * Range point = Range.at(new Position(2, 3, 10));
 * }</pre>
 * 
 * @param start the start position (inclusive)
 * @param end the end position (exclusive)
 */
public record Range(Position start, Position end) {
    
    public Range {
        Objects.requireNonNull(start, "start position cannot be null");
        Objects.requireNonNull(end, "end position cannot be null");
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException(
                "start position must be <= end position: " + start + " > " + end
            );
        }
    }
    
    /**
     * Create a zero-width range at the given position.
     * Useful for pointing to a specific location without spanning multiple characters.
     */
    public static Range at(Position pos) {
        return new Range(pos, pos);
    }
    
    /**
     * Check if this range contains a position.
     */
    public boolean contains(Position pos) {
        return start.compareTo(pos) <= 0 && pos.compareTo(end) < 0;
    }
    
    /**
     * Get the length of this range in characters.
     */
    public int length() {
        return end.offset() - start.offset();
    }
    
    @Override
    public String toString() {
        if (start.equals(end)) {
            return start.toString();
        }
        return start + "-" + end;
    }
}
