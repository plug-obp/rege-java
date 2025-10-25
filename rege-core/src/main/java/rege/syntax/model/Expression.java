package rege.syntax.model;

/**
 * Base sealed interface for all regular expression syntax elements.
 * Permits specific expression types and provides common operations.
 */
public sealed interface Expression permits Terminal, Composite {
    
    /**
     * Accept a visitor for the visitor pattern.
     * 
     * @param visitor the visitor to accept
     * @param input additional input parameter for the visitor
     * @param <T> the input type
     * @param <R> the return type
     * @return the result of the visitor
     */
    <T, R> R accept(Visitor<T, R> visitor, T input);
    
    /**
     * Create a union of this expression with another.
     * 
     * @param other the other expression
     * @return a new union expression, or a simplified form
     */
    default Expression union(Expression other) {
        if (this == EMPTY) return other; // identity law: ∅ | A = A
        if (other == EMPTY) return this; // identity law: A | ∅ = A
        if (this.equals(other)) return this; // idempotent law: A | A = A
        
        // Absorption laws: A | (A | B) = A | B, A | (B | A) = A | B
        if (other instanceof Union u) {
            if (this.equals(u.lhs()) || this.equals(u.rhs())) {
                return other; // A is already in the union, so A | (A | B) = A | B
            }
        }
        if (this instanceof Union u) {
            if (other.equals(u.lhs()) || other.equals(u.rhs())) {
                return this; // B is already in the union, so (A | B) | B = A | B
            }
        }
        
        return this.hashCode() < other.hashCode() ? new Union(this, other) : new Union(other, this);  // order canonically
    }
    
    /**
     * Concatenate this expression with another.
     * 
     * @param other the other expression
     * @return a new concatenation expression, or a simplified form
     */
    default Expression concat(Expression other) {
        if (this == EMPTY || other == EMPTY) return EMPTY; // annihilator law: ∅A = A∅ = ∅
        if (this == EPSILON) return other; // εA = A
        if (other == EPSILON) return this; // Aε = A
        return new Concatenation(this, other);
    }
    
    /**
     * Apply Kleene star to this expression.
     * 
     * @return a new Kleene star expression, or this if already starred
     */
    default Expression star() {
        if (this == EMPTY) return EPSILON;  // ∅* = ε
        if (this == EPSILON) return EPSILON;  // ε* = ε
        if (this instanceof KleeneStar) return this; // already starred
        return new KleeneStar(this);
    }
    /**
     * Constant instance of the empty expression.
     */
    Expression EMPTY = Empty.instance();
    /**
     * Empty expression (matches nothing).
     * @return the empty constant
     */
    default Expression empty() {
        return EMPTY;
    }

    /**
     * Constant instance of the epsilon expression.
     */
    Expression EPSILON = Epsilon.instance();
    /**
     * Epsilon expression (matches the empty string).
     * @return the epsilon constant
     */
    default Expression epsilon() {
        return EPSILON;
    }

    /*
     * Token factory method.
     * @param value the token value (non-empty)
     * @return a new Token expression
     */
    default Token token(String value) {
        return new Token(value);
    }

}
