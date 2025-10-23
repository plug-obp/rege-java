package rege.syntax.model;

import java.util.Objects;

/**
 * A Kleene star operation on an expression (expression*).
 * This is an immutable record representing zero or more repetitions of an expression.
 * 
 * @param expression the expression to be repeated
 */
public record KleeneStar(Expression expression) implements Composite {
    
    public KleeneStar {
        Objects.requireNonNull(expression, "Expression cannot be null");
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitKleeneStar(this, input);
    }
    
    @Override
    public String toString() {
        return "(" + expression + ")*";
    }
}
