package rege.modelchecker;

import obp3.modelchecking.EmptinessCheckerAnswer;
import obp3.modelchecking.EmptinessCheckerStatus;
import obp3.modelchecking.tools.XModelCheckerBuilder;
import obp3.runtime.IExecutable;
import obp3.runtime.sli.DependentSemanticRelation;
import obp3.runtime.sli.SemanticRelation;
import obp3.runtime.sli.Step;
import obp3.sli.core.operators.product.Product;
import obp3.traversal.dfs.DepthFirstTraversal;
import rege.reader.infra.ParseResult;
import rege.semantics.Brzozowski;
import rege.semantics.RegeDependentSemantics;
import rege.syntax.RegeReader;
import rege.syntax.model.Expression;

import java.util.function.BiPredicate;

/**
 * A model checker for verifying properties expressed as regular expressions against systems
 * with Step-based semantics.
 *
 * <p>This class provides a bridge between the OBP3 model checking framework and regular
 * expression-based property specifications. It uses Brzozowski derivatives to efficiently
 * check whether a system satisfies a temporal property expressed as a regular expression
 * over atomic propositions.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a model with SLI semantics
 * SemanticRelation<MyAction, MyContext> modelSemantics = ...;
 *
 * // Define how to evaluate atomic propositions on transition steps
 * BiPredicate<String, Step<MyAction, MyContext>> evaluator =
 *     (prop, step) -> switch(prop) {
 *         case "error" -> step.end().hasError();
 *         case "true" -> true;
 *         default -> false;
 *     };
 *
 * // Create a model checker to find traces reaching an error state
 * var checker = new StepModelChecker<>(modelSemantics, evaluator, "τ[true]* ⋅ τ[error]");
 *
 * // Run the model checker
 * IExecutable<EmptinessCheckerAnswer<?>> executable = checker.modelChecker();
 * var result = executable.runAlone();
 * 
 * // Check if a matching trace was found
 * if (!result.holds) {
 *     System.out.println("Error reachable! Trace length: " + result.trace.size());
 * }
 * }</pre>
 *
 * @param <MA> the type of actions (labels) in the system model
 * @param <MC> the type of configurations (states) in the system model
 *
 * @see Expression
 * @see Brzozowski
 * @see DependentSemantics
 */
public class StepModelChecker<MA, MC> {
    /** The semantic relation defining the system model's behavior. */
    SemanticRelation<MA, MC> modelSemantics;
    
    /** Evaluator for atomic propositions on system steps. */
    BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator;

    /** The regular expression representing the property to verify. */
    Expression propertyModel;

    /** The traversal algorithm to use for state space exploration. */
    DepthFirstTraversal.Algorithm traversalAlgorithm;
    
    /** The maximum depth bound for exploration (-1 for unbounded). */
    int depthBound;

    /**
     * Constructs a model checker with a property specified as a string.
     *
     * <p>The property string is parsed using {@link RegeReader} and must be a valid
     * regular expression. If parsing fails, an {@link IllegalArgumentException} is thrown.
     *
     * <p>This constructor uses default options:
     * <ul>
     *   <li>Traversal algorithm: {@link DepthFirstTraversal.Algorithm#WHILE}</li>
     *   <li>Depth bound: -1 (unbounded)</li>
     * </ul>
     *
     * @param modelSemantics the semantic relation defining the system model
     * @param atomicPropositionEvaluator predicate to evaluate atomic propositions on steps
     * @param property the property to verify, expressed as a regular expression string
     * @throws IllegalArgumentException if the property string cannot be parsed
     */
    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            String property) {
        var model = RegeReader.parse(property);
        switch (model) {
            case ParseResult.Success<Expression> success -> {
                this.modelSemantics = modelSemantics;
                this.atomicPropositionEvaluator = atomicPropositionEvaluator;
                this.propertyModel = success.value();
                this.traversalAlgorithm = DepthFirstTraversal.Algorithm.WHILE;
                this.depthBound = -1;
            }
            case ParseResult.Failure<Expression> failure -> {
                throw new IllegalArgumentException("Failed to parse property: " + failure.formatErrors());
            }
        }
    }

    /**
     * Constructs a model checker with a pre-parsed property expression.
     *
     * <p>This constructor uses default options:
     * <ul>
     *   <li>Traversal algorithm: {@link DepthFirstTraversal.Algorithm#WHILE}</li>
     *   <li>Depth bound: -1 (unbounded)</li>
     * </ul>
     *
     * @param modelSemantics the semantic relation defining the system model
     * @param atomicPropositionEvaluator predicate to evaluate atomic propositions on steps
     * @param propertyModel the property to verify, as a parsed regular expression
     */
    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            Expression propertyModel) {
        this(
                modelSemantics,
                atomicPropositionEvaluator,
                propertyModel,
                DepthFirstTraversal.Algorithm.WHILE,
                -1
        );
    }

    /**
     * Constructs a model checker with full configuration options.
     *
     * <p>This constructor allows complete control over the model checking process,
     * including the traversal algorithm and depth bound for state space exploration.
     *
     * @param modelSemantics the semantic relation defining the system model
     * @param atomicPropositionEvaluator predicate to evaluate atomic propositions on steps
     * @param propertyModel the property to verify, as a parsed regular expression
     * @param traversal the depth-first traversal algorithm to use
     * @param depthBound maximum depth for exploration (-1 for unbounded)
     */
    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            Expression propertyModel,
            DepthFirstTraversal.Algorithm traversal,
            int depthBound) {
        this.modelSemantics = modelSemantics;
        this.atomicPropositionEvaluator = atomicPropositionEvaluator;
        this.propertyModel = propertyModel;
        this.traversalAlgorithm = traversal;
        this.depthBound = depthBound;
    }

    DependentSemanticRelation<Step<MA, MC>, Brzozowski<Step<MA, MC>>, Expression> propertySemanticsProvider(BiPredicate<String, Step<MA, MC>> atomEval) {
        return new DependentSemantics<>(propertyModel, atomicPropositionEvaluator);
    }

    /**
     * Creates an executable model checker for verifying the property against the system.
     *
     * <p>This method constructs a product automaton between the system model and the
     * property automaton (derived from the regular expression using Brzozowski derivatives).
     * The resulting executable can be run to determine whether a trace matching the property exists.
     *
     * <p>The model checking algorithm explores the product state space looking for a trace
     * that matches the property specification. The result's {@code holds} field indicates:
     * <ul>
     *   <li>{@code holds = false}: A matching trace was found ({@code trace} contains the witness)
     *   <li>{@code holds = true}: No matching trace exists in the explored state space
     * </ul>
     *
     * @return an executable that performs the model checking when run
     */
    public IExecutable<EmptinessCheckerStatus, EmptinessCheckerAnswer<Product<MC, Expression>>> modelChecker() {
//        var propertySemantics = new DependentSemantics<>(propertyModel, atomicPropositionEvaluator);
        var builder =
                new XModelCheckerBuilder<MA, MC, Brzozowski<Step<MA, MC>>, Expression>()
                        .modelSemantics(modelSemantics)
                        .propertySemantics(this::propertySemanticsProvider)
                        .acceptingPredicateForProduct((c, sem) -> RegeDependentSemantics.isAccepting(c.r()))
                        .buchi(false)
                        .traversalStrategy(traversalAlgorithm)
                        .depthBound(depthBound);
        return builder.modelChecker();
    }
}