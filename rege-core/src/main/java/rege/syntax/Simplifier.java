package rege.syntax.model;

/**
 * A visitor that simplifies regular expressions using algebraic laws.
 * <p>
 * This simplifier implements the following mathematical laws:
 * <ul>
 *   <li><b>Union Laws:</b>
 *     <ul>
 *       <li>Identity: ∅ | A = A | ∅ = A</li>
 *       <li>Idempotent: A | A = A</li>
 *     </ul>
 *   </li>
 *   <li><b>Concatenation Laws:</b>
 *     <ul>
 *       <li>Annihilator: ∅A = A∅ = ∅</li>
 *       <li>Identity: εA = Aε = A</li>
 *     </ul>
 *   </li>
 *   <li><b>Kleene Star Laws:</b>
 *     <ul>
 *       <li>∅* = ε</li>
 *       <li>ε* = ε</li>
 *       <li>(A*)* = A*</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * The simplifier recursively simplifies all sub-expressions first (bottom-up),
 * then applies simplification rules at each level.
 * <p>
 * Usage:
 * <pre>{@code
 * Expression expr = new KleeneStar(new KleeneStar(new Token("a")));
 * Expression simplified = expr.accept(new Simplifier(), null);
 * // simplified is now KleeneStar(Token("a"))
 * }</pre>
 */
public class Simplifier implements Visitor<Void, Expression> {
    
    /**
     * Creates a new Simplifier visitor.
     */
    public Simplifier() {
    }
    
    /**
     * Simplify the given expression.
     * 
     * @param expression the expression to simplify
     * @return the simplified expression
     */
    public static Expression simplify(Expression expression) {
        return expression.accept(new Simplifier(), null);
    }
    
    @Override
    public Expression visitExpression(Expression expression, Void input) {
        // Default: return as-is
        return expression;
    }
    
    @Override
    public Expression visitToken(Token token, Void input) {
        // Tokens are already in simplest form
        return token;
    }
    
    @Override
    public Expression visitEmpty(Empty empty, Void input) {
        // Empty is already in simplest form
        return empty;
    }
    
    @Override
    public Expression visitEpsilon(Epsilon epsilon, Void input) {
        // Epsilon is already in simplest form
        return epsilon;
    }
    
    @Override
    public Expression visitUnion(Union union, Void input) {
        // First, recursively simplify both sides (bottom-up)
        Expression left = union.lhs().accept(this, input);
        Expression right = union.rhs().accept(this, input);
        
        // Apply union simplification rules:
        
        // Identity law: ∅ | A = A
        if (left == Expression.EMPTY) {
            return right;
        }
        
        // Identity law: A | ∅ = A
        if (right == Expression.EMPTY) {
            return left;
        }
        
        // Idempotent law: A | A = A
        if (left.equals(right)) {
            return left;
        }
        
        // Absorption laws: A | (A | B) = A | B, A | (B | A) = A | B
        // Check if left contains right (recursively)
        if (containsInUnion(left, right)) {
            return left; // Right is already contained in left
        }
        
        // Check if right contains left (recursively)
        if (containsInUnion(right, left)) {
            return right; // Left is already contained in right
        }
        
        // If simplified forms differ from original, create new Union
        if (left != union.lhs() || right != union.rhs()) {
            return new Union(left, right);
        }
        
        // No simplification possible
        return union;
    }
    
    /**
     * Check if an expression contains another expression within a union tree.
     * This recursively searches through nested unions.
     * 
     * @param container the expression to search in
     * @param target the expression to search for
     * @return true if container contains target in its union tree
     */
    private boolean containsInUnion(Expression container, Expression target) {
        if (container.equals(target)) {
            return true;
        }
        
        if (container instanceof Union u) {
            // Recursively check both sides of the union
            return containsInUnion(u.lhs(), target) || containsInUnion(u.rhs(), target);
        }
        
        return false;
    }
    
    @Override
    public Expression visitConcatenation(Concatenation concatenation, Void input) {
        // First, recursively simplify both sides (bottom-up)
        Expression left = concatenation.lhs().accept(this, input);
        Expression right = concatenation.rhs().accept(this, input);
        
        // Apply concatenation simplification rules:
        
        // Annihilator law: ∅A = ∅
        if (left == Expression.EMPTY) {
            return Expression.EMPTY;
        }
        
        // Annihilator law: A∅ = ∅
        if (right == Expression.EMPTY) {
            return Expression.EMPTY;
        }
        
        // Identity law: εA = A
        if (left == Expression.EPSILON) {
            return right;
        }
        
        // Identity law: Aε = A
        if (right == Expression.EPSILON) {
            return left;
        }
        
        // If simplified forms differ from original, create new Concatenation
        if (left != concatenation.lhs() || right != concatenation.rhs()) {
            return new Concatenation(left, right);
        }
        
        // No simplification possible
        return concatenation;
    }
    
    @Override
    public Expression visitKleeneStar(KleeneStar kleeneStar, Void input) {
        // First, recursively simplify the inner expression (bottom-up)
        Expression inner = kleeneStar.expression().accept(this, input);
        
        // Apply Kleene star simplification rules:
        
        // ∅* = ε
        if (inner == Expression.EMPTY) {
            return Expression.EPSILON;
        }
        
        // ε* = ε
        if (inner == Expression.EPSILON) {
            return Expression.EPSILON;
        }
        
        // (A*)* = A*
        if (inner instanceof KleeneStar) {
            return inner;
        }
        
        // If simplified form differs from original, create new KleeneStar
        if (inner != kleeneStar.expression()) {
            return new KleeneStar(inner);
        }
        
        // No simplification possible
        return kleeneStar;
    }
}
