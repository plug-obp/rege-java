package rege.syntax.model;

import java.util.Objects;

/**
 * A terminal token with a specific value.
 * This is an immutable record representing a single token in the regular expression.
 * 
 * @param value the token value
 */
public record Token(String value) implements Terminal {
    
    public Token {
        Objects.requireNonNull(value, "Token value cannot be null");
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitToken(this, input);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
