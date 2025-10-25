package rege.syntax.model;

import java.util.Objects;

/**
 * A concatenation of two expressions (lhs · rhs).
 * This is an immutable record representing the sequential composition of two expressions.
 * 
 * @param lhs the left-hand side expression
 * @param rhs the right-hand side expression
 */
public record Concatenation(Expression lhs, Expression rhs) implements Composite {
    
    /**
     * Compact constructor that validates both expressions are non-null.
     */
    public Concatenation {
        Objects.requireNonNull(lhs, "Left-hand side expression cannot be null");
        Objects.requireNonNull(rhs, "Right-hand side expression cannot be null");
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitConcatenation(this, input);
    }
    
    @Override
    public String toString() {
        return "(" + lhs + " · " + rhs + ")";
    }
}
