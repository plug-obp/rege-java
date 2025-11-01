package rege.modelchecker;

import obp3.runtime.sli.DependentSemanticRelation;
import rege.semantics.Brzozowski;
import rege.semantics.RegeDependentSemantics;
import rege.syntax.model.Expression;

import java.util.function.BiPredicate;

/**
 * Adapter class that bridges {@link RegeDependentSemantics} with the OBP3
 * {@link DependentSemanticRelation} interface.
 *
 * <p>This class enables regular expression-based properties to be used within the
 * OBP3 model checking framework. It combines Brzozowski derivative semantics for
 * regular expressions with context-dependent evaluation of atomic propositions.
 *
 * <p>The dependent semantics uses the provided evaluator to determine the truth
 * value of atomic propositions in specific contexts (of type {@code T}), allowing
 * properties to be checked against concrete system states or configurations.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a regular expression property
 * Expression property = RegeReader.parse("a . b*").value();
 *
 * // Define how to evaluate propositions on configurations
 * BiPredicate<String, MyConfig> evaluator =
 *     (prop, config) -> config.hasProperty(prop);
 *
 * // Create dependent semantics
 * var semantics = new DependentSemantics<>(property, evaluator);
 * }</pre>
 *
 * @param <T> the type of context in which atomic propositions are evaluated
 *           (typically a system state or configuration)
 *
 * @see RegeDependentSemantics
 * @see Brzozowski
 * @see DependentSemanticRelation
 */
public class DependentSemantics<T>
        extends RegeDependentSemantics<T>
        implements DependentSemanticRelation<T, Brzozowski<T>, Expression> {

    /**
     * Constructs dependent semantics for a regular expression property.
     *
     * <p>The evaluator is used to determine the truth value of atomic propositions
     * (represented as strings) in specific contexts of type {@code T}. When model
     * checking, this evaluator will be called for each atomic proposition in the
     * property and each state in the system.
     *
     * @param expression the regular expression representing the property
     * @param evaluator predicate that evaluates atomic propositions in context
     */
    public DependentSemantics(Expression expression, BiPredicate<String, T> evaluator) {
        super(expression, evaluator);
    }
}
