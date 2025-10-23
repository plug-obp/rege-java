package rege.syntax.model;

/**
 * Sealed interface for terminal expressions (leaf nodes in the syntax tree).
 * Permits Token, Empty, and Epsilon.
 */
public sealed interface Terminal extends Expression permits Token, Empty, Epsilon {
    
    @Override
    default <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitTerminal(this, input);
    }
}
