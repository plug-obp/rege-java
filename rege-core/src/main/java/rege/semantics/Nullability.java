package rege.semantics;

import rege.syntax.model.*;

/**
 * Visitor that computes the nullability of a regular expression.
 * 
 * <p>An expression is nullable if it accepts the empty string (ε).
 * This is also denoted as Δ(E) or ν(E) in the literature.
 * 
 * <p><b>Nullability Rules:</b>
 * <ul>
 *   <li>ν(∅) = false</li>
 *   <li>ν(ε) = true</li>
 *   <li>ν(τ[v]) = false</li>
 *   <li>ν(A⋅B) = ν(A) ∧ ν(B)</li>
 *   <li>ν(A|B) = ν(A) ∨ ν(B)</li>
 *   <li>ν(A*) = true</li>
 * </ul>
 * 
 * @see Brzozowski
 */
public class Nullability implements Visitor<Void, Boolean> {
    
    /**
     * Empty language is not nullable (doesn't accept ε).
     */
    @Override
    public Boolean visitEmpty(Empty empty, Void input) {
        return false;
    }
    
    /**
     * Epsilon is nullable (accepts the empty string).
     */
    @Override
    public Boolean visitEpsilon(Epsilon epsilon, Void input) {
        return true;
    }
    
    /**
     * Tokens are not nullable (require at least one symbol).
     */
    @Override
    public Boolean visitToken(Token token, Void input) {
        return false;
    }
    
    /**
     * Concatenation is nullable if both operands are nullable.
     * 
     * @return ν(A⋅B) = ν(A) ∧ ν(B)
     */
    @Override
    public Boolean visitConcatenation(Concatenation concatenation, Void input) {
        return concatenation.lhs().accept(this, input) 
            && concatenation.rhs().accept(this, input);
    }
    
    /**
     * Union is nullable if either operand is nullable.
     * 
     * @return ν(A|B) = ν(A) ∨ ν(B)
     */
    @Override
    public Boolean visitUnion(Union union, Void input) {
        return union.lhs().accept(this, input) 
            || union.rhs().accept(this, input);
    }
    
    /**
     * Kleene star is always nullable (accepts ε via zero repetitions).
     * 
     * @return ν(A*) = true
     */
    @Override
    public Boolean visitKleeneStar(KleeneStar kleeneStar, Void input) {
        return true;
    }
    
    /**
     * Check if an expression is nullable (accepts empty string).
     * 
     * @param expression the expression to check
     * @return true if the expression accepts ε
     */
    public static boolean isNullable(Expression expression) {
        return expression.accept(new Nullability(), null);
    }
}
