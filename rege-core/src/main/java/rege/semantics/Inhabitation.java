package rege.semantics;

import rege.syntax.model.*;

/**
 * Visitor that checks if a regular expression's language is inhabited (non-empty).
 * 
 * <p>A language is inhabited if it contains at least one string.
 * This is different from nullability - a language can be inhabited without accepting ε.
 * 
 * <p><b>Inhabitation Rules:</b>
 * <ul>
 *   <li>inhabited(∅) = false (empty language)</li>
 *   <li>inhabited(ε) = true (contains "")</li>
 *   <li>inhabited(τ[v]) = true (contains one string)</li>
 *   <li>inhabited(A⋅B) = inhabited(A) ∧ inhabited(B)</li>
 *   <li>inhabited(A|B) = inhabited(A) ∨ inhabited(B)</li>
 *   <li>inhabited(A*) = true (always contains "" via zero repetitions)</li>
 * </ul>
 * 
 * <p><b>Examples:</b>
 * <ul>
 *   <li>∅ is not inhabited</li>
 *   <li>ε is inhabited (L = {""})</li>
 *   <li>τ[a] is inhabited (L = {"a"})</li>
 *   <li>∅⋅τ[a] is not inhabited (L = {})</li>
 *   <li>∅* is inhabited (L = {""}) - even though ∅ itself is not</li>
 * </ul>
 */
public class Inhabitation implements Visitor<Void, Boolean> {
    
    /**
     * Creates a new Inhabitation visitor.
     */
    public Inhabitation() {
    }
    
    /**
     * Empty language is not inhabited.
     * 
     * @return false - L(∅) = {}
     */
    @Override
    public Boolean visitEmpty(Empty empty, Void input) {
        return false;
    }
    
    /**
     * Epsilon is inhabited (contains the empty string).
     * 
     * @return true - L(ε) = {""}
     */
    @Override
    public Boolean visitEpsilon(Epsilon epsilon, Void input) {
        return true;
    }
    
    /**
     * Tokens are inhabited (each token represents a non-empty language).
     * 
     * @return true - L(τ[v]) = {v}
     */
    @Override
    public Boolean visitToken(Token token, Void input) {
        return true;
    }
    
    /**
     * Concatenation is inhabited if both operands are inhabited.
     * 
     * <p>L(A⋅B) is non-empty iff both L(A) and L(B) are non-empty.
     * 
     * @return inhabited(A⋅B) = inhabited(A) ∧ inhabited(B)
     */
    @Override
    public Boolean visitConcatenation(Concatenation concatenation, Void input) {
        return concatenation.lhs().accept(this, input) 
            && concatenation.rhs().accept(this, input);
    }
    
    /**
     * Union is inhabited if either operand is inhabited.
     * 
     * <p>L(A|B) is non-empty if either L(A) or L(B) is non-empty.
     * 
     * @return inhabited(A|B) = inhabited(A) ∨ inhabited(B)
     */
    @Override
    public Boolean visitUnion(Union union, Void input) {
        return union.lhs().accept(this, input) 
            || union.rhs().accept(this, input);
    }
    
    /**
     * Kleene star is always inhabited (accepts ε via zero repetitions).
     * 
     * <p>Even if L(A) = {}, we have L(A*) = {""} because the star allows
     * zero repetitions.
     * 
     * @return true - L(A*) always contains ""
     */
    @Override
    public Boolean visitKleeneStar(KleeneStar kleeneStar, Void input) {
        return true;
    }
    
    /**
     * Check if an expression's language is inhabited (contains at least one string).
     * 
     * @param expression the expression to check
     * @return true if L(expression) ≠ {}
     */
    public static boolean isInhabited(Expression expression) {
        return expression.accept(new Inhabitation(), null);
    }
}
