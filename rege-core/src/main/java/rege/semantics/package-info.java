/**
 * Brzozowski derivative-based semantics for regular expressions.
 * 
 * <p>This package implements formal semantics using Brzozowski derivatives,
 * a mathematical technique for regular expression matching and manipulation.
 * 
 * <h2>Brzozowski Derivatives</h2>
 * 
 * <p>The derivative of a regular expression E with respect to a symbol a,
 * denoted D<sub>a</sub>(E), represents the language that remains after matching
 * the first symbol a.
 * 
 * <h3>Derivative Rules</h3>
 * <p>The {@link rege.semantics.Brzozowski} visitor implements these rules:
 * 
 * <table border="1">
 *   <caption>Derivative Rules</caption>
 *   <tr><th>Expression</th><th>Derivative D<sub>a</sub>(E)</th></tr>
 *   <tr><td>∅</td><td>∅</td></tr>
 *   <tr><td>ε</td><td>∅</td></tr>
 *   <tr><td>τ[v]</td><td>ε if eval(v,a) else ∅</td></tr>
 *   <tr><td>B⋅C</td><td>D<sub>a</sub>(B)⋅C | ν(B)⋅D<sub>a</sub>(C)</td></tr>
 *   <tr><td>A|B</td><td>D<sub>a</sub>(A) | D<sub>a</sub>(B)</td></tr>
 *   <tr><td>A*</td><td>D<sub>a</sub>(A)⋅A*</td></tr>
 * </table>
 * 
 * <p>where ν(E) is the nullability of E (whether it accepts ε).
 * 
 * <h3>Heterogeneous Semantics</h3>
 * <p>The derivative implementation is generic over input type {@code T} and
 * uses an external evaluator to determine token matches:
 * 
 * <pre>{@code
 * // Character-based matching
 * BiPredicate<String, Character> charEval = 
 *     (token, ch) -> token.equals(String.valueOf(ch));
 * Brzozowski<Character> derivator = new Brzozowski<>(charEval);
 * 
 * // String matching
 * BiPredicate<String, String> stringEval = String::equals;
 * Brzozowski<String> derivator2 = new Brzozowski<>(stringEval);
 * 
 * // Regex matching
 * BiPredicate<String, String> regexEval = 
 *     (token, str) -> str.matches(token);
 * Brzozowski<String> derivator3 = new Brzozowski<>(regexEval);
 * }</pre>
 * 
 * <h2>Supporting Visitors</h2>
 * 
 * <h3>Nullability</h3>
 * <p>{@link rege.semantics.Nullability} computes whether an expression accepts
 * the empty string (ε). This is denoted ν(E) or Δ(E) in the literature.
 * 
 * <p><b>Rules:</b>
 * <ul>
 *   <li>ν(∅) = false</li>
 *   <li>ν(ε) = true</li>
 *   <li>ν(τ[v]) = false</li>
 *   <li>ν(A⋅B) = ν(A) ∧ ν(B)</li>
 *   <li>ν(A|B) = ν(A) ∨ ν(B)</li>
 *   <li>ν(A*) = true</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression expr = new KleeneStar(new Token("a"));
 * boolean nullable = Nullability.isNullable(expr); // true (a* accepts ε)
 * 
 * Expression expr2 = new Token("a");
 * boolean nullable2 = Nullability.isNullable(expr2); // false (a does not accept ε)
 * }</pre>
 * 
 * <h3>Inhabitation</h3>
 * <p>{@link rege.semantics.Inhabitation} checks whether an expression's language
 * is non-empty (contains at least one string).
 * 
 * <p><b>Rules:</b>
 * <ul>
 *   <li>inhabited(∅) = false</li>
 *   <li>inhabited(ε) = true</li>
 *   <li>inhabited(τ[v]) = true</li>
 *   <li>inhabited(A⋅B) = inhabited(A) ∧ inhabited(B)</li>
 *   <li>inhabited(A|B) = inhabited(A) ∨ inhabited(B)</li>
 *   <li>inhabited(A*) = true</li>
 * </ul>
 * 
 * <p><b>Difference from Nullability:</b>
 * <ul>
 *   <li>Token "a": nullable=false, inhabited=true</li>
 *   <li>Empty ∅: nullable=false, inhabited=false</li>
 *   <li>Epsilon ε: nullable=true, inhabited=true</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression expr = new Concatenation(Expression.EMPTY, new Token("a"));
 * boolean inhabited = Inhabitation.isInhabited(expr); // false (∅⋅a has empty language)
 * 
 * Expression expr2 = new KleeneStar(Expression.EMPTY);
 * boolean inhabited2 = Inhabitation.isInhabited(expr2); // true (∅* = ε)
 * }</pre>
 * 
 * <h2>Dependent Semantics Framework</h2>
 * 
 * <h3>RegeDependentSemantics</h3>
 * <p>{@link rege.semantics.RegeDependentSemantics} provides a framework for
 * step-by-step regular expression matching where actions depend on the current
 * configuration's inhabitation.
 * 
 * <p><b>Concepts:</b>
 * <ul>
 *   <li><b>Configuration:</b> A regular expression representing remaining work</li>
 *   <li><b>Action:</b> A derivative computation consuming one input symbol</li>
 *   <li><b>Execution:</b> Applying the derivative to get a new configuration</li>
 * </ul>
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Pattern: (a|b)*⋅c
 * Expression pattern = new Concatenation(
 *     new KleeneStar(new Union(new Token("a"), new Token("b"))),
 *     new Token("c")
 * );
 * 
 * // Character-based evaluator
 * BiPredicate<String, Character> evaluator = 
 *     (token, ch) -> token.equals(String.valueOf(ch));
 * 
 * // Create semantics
 * RegeDependentSemantics<Character> semantics = 
 *     new RegeDependentSemantics<>(pattern, evaluator);
 * 
 * // Match input "abc"
 * boolean accepted = semantics.accepts(List.of('a', 'b', 'c')); // true
 * boolean rejected = semantics.accepts(List.of('a', 'b', 'd')); // false
 * }</pre>
 * 
 * <h3>Step-by-Step Processing</h3>
 * <p>For interactive or debugging use, process input step by step:
 * 
 * <pre>{@code
 * RegeDependentSemantics<Character> semantics = ...;
 * 
 * List<Expression> configs = semantics.initial();
 * for (char ch : "abc".toCharArray()) {
 *     Expression current = configs.get(0);
 *     
 *     // Check available actions
 *     List<Brzozowski<Character>> actions = semantics.actions(ch, current);
 *     if (actions.isEmpty()) {
 *         // No actions available - language became empty
 *         break;
 *     }
 *     
 *     // Execute action (compute derivative)
 *     configs = semantics.execute(actions.get(0), ch, current);
 * }
 * 
 * // Check if final configuration accepts
 * boolean accepted = RegeDependentSemantics.isAccepting(configs.get(0));
 * }</pre>
 * 
 * <h2>Mathematical Foundations</h2>
 * 
 * <p>This package implements the theory from:
 * <ul>
 *   <li>Brzozowski, J. A. (1964). "Derivatives of Regular Expressions"</li>
 *   <li>Owens, S., Reppy, J., and Turon, A. (2009). "Regular expression derivatives re-examined"</li>
 * </ul>
 * 
 * <h3>Key Properties</h3>
 * <ul>
 *   <li><b>Correctness:</b> w ∈ L(E) ⟺ ν(D<sub>w</sub>(E)) where D<sub>w</sub> is iterated derivatives</li>
 *   <li><b>Compositionality:</b> D<sub>a</sub>(E₁ op E₂) defined in terms of D<sub>a</sub>(E₁) and D<sub>a</sub>(E₂)</li>
 *   <li><b>Simplification:</b> Smart constructors prevent derivative explosion</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * 
 * <p>The derivative approach creates new expression trees for each input symbol.
 * Performance optimizations:
 * <ul>
 *   <li><b>Smart constructors:</b> Algebraic laws applied during construction</li>
 *   <li><b>Simplification:</b> Periodic simplification prevents tree growth</li>
 *   <li><b>Inhabitation pruning:</b> Actions only available for non-empty languages</li>
 * </ul>
 * 
 * <p>For large-scale matching, consider:
 * <ul>
 *   <li>Caching derivatives in {@code RegeDependentSemantics}</li>
 *   <li>Hash-consing expression trees for structural sharing</li>
 *   <li>Using simplification periodically to prevent expression growth</li>
 * </ul>
 * 
 * @see rege.semantics.Brzozowski
 * @see rege.semantics.Nullability
 * @see rege.semantics.Inhabitation
 * @see rege.semantics.RegeDependentSemantics
 * @see rege.syntax.model
 */
package rege.semantics;
