package rege.syntax.model;

/**
 * Visitor interface for traversing and processing expression trees.
 * 
 * @param <T> the input type for visit methods
 * @param <R> the return type for visit methods
 */
public interface Visitor<T, R> {
    
    /**
     * Visit a generic expression.
     * 
     * @param expression the expression to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitExpression(Expression expression, T input) {
        return null;
    }
    
    /**
     * Visit a terminal expression.
     * 
     * @param terminal the terminal to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitTerminal(Terminal terminal, T input) {
        return visitExpression(terminal, input);
    }
    
    /**
     * Visit a token.
     * 
     * @param token the token to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitToken(Token token, T input) {
        return visitTerminal(token, input);
    }
    
    /**
     * Visit an empty terminal.
     * 
     * @param empty the empty terminal to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitEmpty(Empty empty, T input) {
        return visitTerminal(empty, input);
    }
    
    /**
     * Visit an epsilon terminal.
     * 
     * @param epsilon the epsilon terminal to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitEpsilon(Epsilon epsilon, T input) {
        return visitTerminal(epsilon, input);
    }
    
    /**
     * Visit a composite expression.
     * 
     * @param composite the composite expression to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitComposite(Composite composite, T input) {
        return visitExpression(composite, input);
    }
    
    /**
     * Visit a concatenation.
     * 
     * @param concatenation the concatenation to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitConcatenation(Concatenation concatenation, T input) {
        return visitComposite(concatenation, input);
    }
    
    /**
     * Visit a union.
     * 
     * @param union the union to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitUnion(Union union, T input) {
        return visitComposite(union, input);
    }
    
    /**
     * Visit a Kleene star.
     * 
     * @param kleeneStar the Kleene star to visit
     * @param input additional input parameter
     * @return the result of the visit
     */
    default R visitKleeneStar(KleeneStar kleeneStar, T input) {
        return visitComposite(kleeneStar, input);
    }
}
