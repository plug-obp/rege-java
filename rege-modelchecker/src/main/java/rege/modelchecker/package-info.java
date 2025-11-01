/**
 * Model checking capabilities for verifying system properties using regular expressions.
 *
 * <p>This package provides integration between regular expression-based property
 * specifications and the OBP3 model checking framework. It enables automated
 * verification of temporal properties expressed as regular expressions over
 * atomic propositions.
 *
 * <h2>Overview</h2>
 *
 * <p>The package offers two main components:
 *
 * <ul>
 *   <li>{@link rege.modelchecker.StepModelChecker} - Main entry point for model checking
 *   <li>{@link rege.modelchecker.DependentSemantics} - Adapter for property semantics
 * </ul>
 *
 * <h2>Key Concepts</h2>
 *
 * <h3>Property Specification</h3>
 *
 * <p>Properties are specified as regular expressions over transition labels.
 * The syntax {@code τ[condition]} denotes a transition step where the given
 * atomic proposition holds. Standard regular expression operators can be used:
 * <ul>
 *   <li>{@code "τ[a] ⋅ τ[b]"} - transition with "a" followed by transition with "b"
 *   <li>{@code "τ[a]*"} - zero or more transitions satisfying "a"
 *   <li>{@code "τ[a] | τ[b]"} - transition with "a" or transition with "b"
 *   <li>{@code "τ[true]* ⋅ τ[goal]"} - any sequence of transitions ending with "goal"
 * </ul>
 *
 * <h3>Atomic Proposition Evaluation</h3>
 *
 * <p>Atomic propositions are strings that are evaluated against system transition steps.
 * The user provides a {@link java.util.function.BiPredicate} that takes an atomic proposition
 * string and a {@link obp3.runtime.sli.Step} (containing source state, action, and target state),
 * and determines whether the proposition holds for that step.
 *
 * <h3>Model Checking Algorithm</h3>
 *
 * <p>The implementation uses:
 * <ul>
 *   <li><b>Brzozowski derivatives</b> for efficient regular expression processing
 *   <li><b>Product automaton</b> construction between system and property
 *   <li><b>Depth-first traversal</b> for state space exploration
 *   <li><b>Language non-emptiness checking</b> to find traces matching the property
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // 1. Define your system model with SLI semantics
 * SemanticRelation<Action, Config> modelSemantics = new MySystemSemantics();
 *
 * // 2. Define how to evaluate atomic propositions on steps
 * BiPredicate<String, Step<Action, Config>> evaluator = (prop, step) -> {
 *     return switch (prop) {
 *         case "ready" -> step.start().isReady();
 *         case "error" -> step.end().hasError();
 *         case "true" -> true;  // matches any step
 *         default -> false;
 *     };
 * };
 *
 * // 3. Specify the property - find a trace reaching an error state
 * String property = "τ[true]* ⋅ τ[error]";  // any steps followed by an error
 *
 * // 4. Create and run the model checker
 * var checker = new StepModelChecker<>(modelSemantics, evaluator, property);
 * IExecutable<EmptinessCheckerAnswer<?>> executable = checker.modelChecker();
 * EmptinessCheckerAnswer<?> result = executable.runAlone();
 *
 * // 5. Check the result
 * if (!result.holds) {
 *     System.out.println("Property matched! Trace found with " + result.trace.size() + " steps");
 * } else {
 *     System.out.println("Property not matched - no trace found");
 * }
 * }</pre>
 *
 * <h2>Integration with OBP3</h2>
 *
 * <p>This package bridges two frameworks:
 * <ul>
 *   <li><b>rege-core</b> - Regular expression syntax and Brzozowski semantics
 *   <li><b>obp3-runtime &amp; obp3-algos</b> - Model checking infrastructure
 * </ul>
 *
 * <p>The integration is achieved through:
 * <ul>
 *   <li>{@link rege.modelchecker.DependentSemantics} implementing OBP3's
 *       {@code DependentSemanticRelation} interface
 *   <li>{@link rege.modelchecker.StepModelChecker} using OBP3's
 *       {@code ModelCheckerBuilder} for product construction
 * </ul>
 *
 * <h2>Advanced Configuration</h2>
 *
 * <p>The {@link rege.modelchecker.StepModelChecker} can be configured with:
 * <ul>
 *   <li><b>Traversal algorithm</b> - Different depth-first search strategies
 *   <li><b>Depth bound</b> - Limit exploration depth (useful for infinite systems)
 * </ul>
 *
 * <pre>{@code
 * var checker = new StepModelChecker<>(
 *     modelSemantics,
 *     evaluator,
 *     propertyExpression,
 *     obp3.traversal.dfs.DepthFirstTraversal.Algorithm.WHILE,  // traversal strategy
 *     100                                        // depth bound
 * );
 * }</pre>
 *
 * @see rege.modelchecker.StepModelChecker
 * @see rege.modelchecker.DependentSemantics
 * @see rege.syntax.model.Expression
 * @see rege.semantics.Brzozowski
 */
package rege.modelchecker;
