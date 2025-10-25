package rege.syntax.model;

import java.util.Objects;

/**
 * A union of two expressions (lhs | rhs).
 * This is an immutable record representing the choice between two expressions.
 * 
 * @param lhs the left-hand side expression
 * @param rhs the right-hand side expression
 */
public record Union(Expression lhs, Expression rhs) implements Composite {
    
    /**
     * Compact constructor that validates both expressions are non-null.
     */
    public Union {
        Objects.requireNonNull(lhs, "Left-hand side expression cannot be null");
        Objects.requireNonNull(rhs, "Right-hand side expression cannot be null");
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitUnion(this, input);
    }
    
    @Override
    public String toString() {
        return "(" + lhs + " | " + rhs + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Union other)) return false;
        return (Objects.equals(lhs, other.lhs) && Objects.equals(rhs, other.rhs)) ||
               (Objects.equals(lhs, other.rhs) && Objects.equals(rhs, other.lhs));
    }

    @Override
    public int hashCode() {
        // Ensure the hash code is independent of the order of lhs and rhs
        // always non-zero and commutative
        int hash1 = Objects.hashCode(lhs);
        int hash2 = Objects.hashCode(rhs);
        return 31 * (hash1 + hash2) + (hash1 * hash2);
    }
}
