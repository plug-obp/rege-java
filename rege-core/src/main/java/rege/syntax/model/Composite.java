package rege.syntax.model;

/**
 * Sealed interface for composite expressions (non-leaf nodes in the syntax tree).
 * Permits Concatenation, Union, and KleeneStar.
 */
public sealed interface Composite extends Expression permits Concatenation, Union, KleeneStar {
    
    @Override
    default <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitComposite(this, input);
    }
}
