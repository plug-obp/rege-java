package rege.semantics;

import rege.syntax.model.*;

import java.util.function.BiPredicate;

/**
 * Brzozowski derivative computation for regular expressions.
 * 
 * <p>The derivative of a regular expression R with respect to a symbol a (denoted D_a(R))
 * represents the language of strings that, when prepended with 'a', are in L(R).
 * 
 * <p>In heterogeneous semantics, tokens evaluate to boolean values using an external evaluator.
 * The evaluator is a function that takes a token value and an input, returning true if the
 * token matches the input.
 * 
 * <p><b>Derivative Rules:</b>
 * <ul>
 *   <li>D_a(∅) = ∅</li>
 *   <li>D_a(ε) = ∅</li>
 *   <li>D_a(τ[v]) = ε if evaluator(v, a) = true, else ∅ (v must be non-empty)</li>
 *   <li>D_a(B⋅C) = D_a(B)⋅C | ν(B)⋅D_a(C)</li>
 *   <li>D_a(B|C) = D_a(B) | D_a(C)</li>
 *   <li>D_a(B*) = D_a(B)⋅B*</li>
 * </ul>
 * 
 * <p><b>Token Values:</b>
 * The evaluator receives token values that are guaranteed to be non-empty.
 * Empty strings are represented using {@link Epsilon}, not {@link Token}.
 * This ensures clear mathematical semantics and prevents ambiguity in derivative computation.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Simple character matching evaluator
 * BiPredicate<String, Character> evaluator = String::equals;
 * 
 * Brzozowski brz = new Brzozowski<>(evaluator);
 * Expression derivative = brz.derivative(expression, 'a');
 * boolean accepts = brz.isNullable(derivative);
 * }</pre>
 * 
 * @param <T> the type of input symbols (e.g., Character, String, etc.)
 * @see Nullability
 * @see Token
 */
public class Brzozowski<T> implements Visitor<T, Expression> {
    
    private final BiPredicate<String, T> evaluator;
    private final Nullability nullability;
    
    /**
     * Create a Brzozowski derivative computer with the given token evaluator.
     * 
     * @param evaluator function that evaluates if a token matches an input symbol.
     *                  Takes (tokenValue, input) and returns true if they match.
     *                  The tokenValue parameter is guaranteed to be non-empty.
     */
    public Brzozowski(BiPredicate<String, T> evaluator) {
        this.evaluator = evaluator;
        this.nullability = new Nullability();
    }
    
    /**
     * Compute the derivative of an expression with respect to an input symbol.
     * 
     * @param expression the regular expression
     * @param input the input symbol
     * @return the derivative expression D_input(expression)
     */
    public Expression derivative(Expression expression, T input) {
        return expression.accept(this, input);
    }
    
    /**
     * Check if an expression is nullable (accepts the empty string).
     * 
     * @param expression the expression to check
     * @return true if the expression accepts ε
     */
    public boolean isNullable(Expression expression) {
        return expression.accept(nullability, null);
    }
    
    /**
     * D_a(∅) = ∅
     * 
     * <p>The derivative of the empty language is always empty.
     */
    @Override
    public Expression visitEmpty(Empty empty, T input) {
        return Expression.EMPTY;
    }
    
    /**
     * D_a(ε) = ∅
     * 
     * <p>The derivative of epsilon is empty (ε only accepts the empty string,
     * so consuming any symbol results in failure).
     */
    @Override
    public Expression visitEpsilon(Epsilon epsilon, T input) {
        return Expression.EMPTY;
    }
    
    /**
     * D_a(τ[v]) = ε if evaluator(v, a) = true, else ∅
     * 
     * <p>If the token matches the input (via the evaluator), the derivative is ε
     * (we've consumed the token, nothing left to match). Otherwise, it's ∅.
     * 
     * <p>The token value is guaranteed to be non-empty by the {@link Token} constructor,
     * so evaluators never need to handle empty strings.
     */
    @Override
    public Expression visitToken(Token token, T input) {
        return evaluator.test(token.value(), input) 
            ? Expression.EPSILON 
            : Expression.EMPTY;
    }
    
    /**
     * D_a(B⋅C) = D_a(B)⋅C | ν(B)⋅D_a(C)
     * 
     * <p>The derivative of concatenation has two cases:
     * <ol>
     *   <li>If B is nullable: D_a(B)⋅C ∪ D_a(C) (can skip B if it accepts ε)</li>
     *   <li>If B is not nullable: D_a(B)⋅C (must consume from B first)</li>
     * </ol>
     * 
     * <p>This uses smart constructors, so if B is nullable and D_a(B) = ∅,
     * the result simplifies to D_a(C).
     */
    @Override
    public Expression visitConcatenation(Concatenation concatenation, T input) {
        Expression lhsDerivative = derivative(concatenation.lhs(), input);
        
        if (isNullable(concatenation.lhs())) {
            // ν(B) = true: D_a(B⋅C) = D_a(B)⋅C | D_a(C)
            Expression rhsDerivative = derivative(concatenation.rhs(), input);
            return lhsDerivative.concat(concatenation.rhs()).union(rhsDerivative);
        } else {
            // ν(B) = false: D_a(B⋅C) = D_a(B)⋅C
            return lhsDerivative.concat(concatenation.rhs());
        }
    }
    
    /**
     * D_a(B|C) = D_a(B) | D_a(C)
     * 
     * <p>The derivative of union is the union of derivatives.
     * This uses smart constructors for automatic simplification.
     */
    @Override
    public Expression visitUnion(Union union, T input) {
        return derivative(union.lhs(), input)
            .union(derivative(union.rhs(), input));
    }
    
    /**
     * D_a(B*) = D_a(B)⋅B*
     * 
     * <p>The derivative of Kleene star: consume one element from B,
     * then we still need to match B* (zero or more additional Bs).
     */
    @Override
    public Expression visitKleeneStar(KleeneStar kleeneStar, T input) {
        return derivative(kleeneStar.expression(), input)
            .concat(kleeneStar);
    }
    
    /**
     * Check if a string is accepted by an expression.
     * 
     * <p>This computes derivatives for each character in the input string,
     * then checks if the final derivative is nullable.
     * 
     * @param expression the regular expression
     * @param input the input string (as iterable of symbols)
     * @return true if the expression accepts the input
     */
    public boolean matches(Expression expression, Iterable<T> input) {
        Expression current = expression;
        for (T symbol : input) {
            current = derivative(current, symbol);
        }
        return isNullable(current);
    }
}
