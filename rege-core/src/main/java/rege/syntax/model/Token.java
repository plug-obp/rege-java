package rege.syntax.model;

import java.util.Objects;

/**
 * Terminal expression representing a token with an associated value.
 * 
 * <p>A token represents a terminal symbol that matches input via an external evaluator.
 * The token value is compared against input symbols using a predicate provided to the
 * {@link rege.semantics.Brzozowski} derivative computation.
 * 
 * <p><b>Mathematical Semantics:</b>
 * <ul>
 *   <li>L(τ[v]) = {v} if evaluator(v, input) = true for some input</li>
 *   <li>Derivative: D_a(τ[v]) = ε if evaluator(v, a) = true, else ∅</li>
 *   <li>Nullability: ν(τ[v]) = false (tokens never accept empty string)</li>
 * </ul>
 * 
 * <p><b>Empty Token Values:</b>
 * Token values <b>must be non-empty</b>. This ensures clear separation between:
 * <ul>
 *   <li><b>τ[v]</b> - matches concrete input via external evaluator (v non-empty)</li>
 *   <li><b>ε</b> - matches empty string (use {@link Expression#EPSILON})</li>
 *   <li><b>∅</b> - matches nothing (use {@link Expression#EMPTY})</li>
 * </ul>
 * 
 * <p>Attempting to create a token with an empty string will throw
 * {@link IllegalArgumentException}. This prevents mathematical ambiguity and
 * ensures evaluators never receive empty token values.
 * 
 * <p><b>Escape Sequences:</b>
 * Token values contain interpreted characters. Special characters are escaped
 * in the textual representation but stored as actual characters:
 * <ul>
 *   <li>Input: {@code τ[a\]b]} → Token stores: {@code "a]b"}</li>
 *   <li>Input: {@code τ[hello\nworld]} → Token stores: {@code "hello\nworld"} (actual newline)</li>
 * </ul>
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Valid tokens
 * Token t1 = new Token("a");
 * Token t2 = new Token("hello");
 * Token t3 = new Token("a]b");  // Contains closing bracket
 * 
 * // Invalid - throws IllegalArgumentException
 * Token empty = new Token("");  // Use Expression.EPSILON instead
 * }</pre>
 * 
 * @param value the token value (must be non-empty)
 * @throws IllegalArgumentException if value is empty
 */
public record Token(String value) implements Terminal {
    
    public Token {
        Objects.requireNonNull(value, "Token value cannot be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                "Token value cannot be empty. Use Expression.EPSILON to represent the empty string."
            );
        }
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
