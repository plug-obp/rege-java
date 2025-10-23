package rege.semantics;

import rege.syntax.model.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Dependent semantics for regular expressions using Brzozowski derivatives.
 * 
 * <p>This class implements a semantic framework where:
 * <ul>
 *   <li><b>Configurations</b> are regular expressions (representing remaining work)</li>
 *   <li><b>Actions</b> are derivative computations (consuming input symbols)</li>
 *   <li><b>Execution</b> computes the derivative, yielding a new configuration</li>
 * </ul>
 * 
 * <p>The semantics is "dependent" because the available actions depend on whether
 * the current configuration's language is inhabited (non-empty).
 * 
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Match strings against pattern (a|b)*c
 * Expression pattern = ...; // (a|b)*⋅c
 * 
 * // Character-based evaluator
 * BiPredicate<String, Character> evaluator = (token, ch) -> 
 *     token.equals(String.valueOf(ch));
 * 
 * RegeDependentSemantics<Character> semantics = 
 *     new RegeDependentSemantics<>(pattern, evaluator);
 * 
 * // Process input "abc"
 * List<Expression> configs = semantics.initial();
 * for (char ch : "abc".toCharArray()) {
 *     List<Brzozowski<Character>> actions = semantics.actions(ch, configs.get(0));
 *     if (!actions.isEmpty()) {
 *         configs = semantics.execute(actions.get(0), ch, configs.get(0));
 *     }
 * }
 * 
 * // Check if final configuration accepts
 * boolean accepted = isAccepting(configs.get(0));
 * }</pre>
 * 
 * @param <T> the type of input symbols (e.g., Character, String, etc.)
 */
public class RegeDependentSemantics<T> {
    
    private final Expression expression;
    private final BiPredicate<String, T> evaluator;
    private final Function<BiPredicate<String, T>, Brzozowski<T>> derivatorBuilder;
    
    /**
     * Create a dependent semantics with a custom derivator builder.
     * 
     * @param expression the initial regular expression
     * @param derivatorBuilder function that creates a Brzozowski instance from an evaluator
     * @param evaluator the token evaluator
     */
    public RegeDependentSemantics(
            Expression expression,
            Function<BiPredicate<String, T>, Brzozowski<T>> derivatorBuilder,
            BiPredicate<String, T> evaluator) {
        this.expression = expression;
        this.evaluator = evaluator;
        this.derivatorBuilder = derivatorBuilder;
    }
    
    /**
     * Create a dependent semantics with the default Brzozowski derivator.
     * 
     * @param expression the initial regular expression
     * @param evaluator the token evaluator
     */
    public RegeDependentSemantics(Expression expression, BiPredicate<String, T> evaluator) {
        this(expression, Brzozowski::new, evaluator);
    }
    
    /**
     * Get the initial configuration (the starting expression).
     * 
     * @return a singleton list containing the initial expression
     */
    public List<Expression> initial() {
        return List.of(expression);
    }
    
    /**
     * Get available actions for a given input and configuration.
     * 
     * <p>Actions are only available if the configuration's language is inhabited
     * (i.e., there are still possible strings to match).
     * 
     * @param input the input symbol (not used in action determination)
     * @param configuration the current expression configuration
     * @return list of available Brzozowski derivators (at most one)
     */
    public List<Brzozowski<T>> actions(T input, Expression configuration) {
        if (Inhabitation.isInhabited(configuration)) {
            return List.of(derivatorBuilder.apply(evaluator));
        }
        return List.of();
    }
    
    /**
     * Execute an action (compute derivative) on a configuration.
     * 
     * @param action the Brzozowski derivator to apply
     * @param input the input symbol to consume
     * @param configuration the current expression configuration
     * @return a singleton list containing the derivative (new configuration)
     */
    public List<Expression> execute(Brzozowski<T> action, T input, Expression configuration) {
        return List.of(configuration.accept(action, input));
    }
    
    /**
     * Process an entire input sequence and return the final configuration.
     * 
     * @param input the sequence of input symbols
     * @return the final configuration after processing all input
     */
    public Expression process(Iterable<T> input) {
        Expression current = expression;
        
        for (T symbol : input) {
            List<Brzozowski<T>> actions = actions(symbol, current);
            if (actions.isEmpty()) {
                // No actions available - language became empty
                return Expression.EMPTY;
            }
            
            List<Expression> next = execute(actions.get(0), symbol, current);
            current = next.get(0);
        }
        
        return current;
    }
    
    /**
     * Check if an input sequence is accepted by the expression.
     * 
     * @param input the sequence of input symbols
     * @return true if the expression accepts the input
     */
    public boolean accepts(Iterable<T> input) {
        Expression finalConfig = process(input);
        return isAccepting(finalConfig);
    }
    
    /**
     * Check if a configuration is accepting (nullable).
     * 
     * <p>A configuration is accepting if its language contains the empty string.
     * 
     * @param configuration the expression configuration
     * @return true if the configuration accepts ε
     */
    public static boolean isAccepting(Expression configuration) {
        return Nullability.isNullable(configuration);
    }
}
