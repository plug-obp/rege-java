/**
 * Core syntax model for regular expressions.
 * 
 * <p>This package provides an immutable, type-safe representation of regular expressions
 * using Java 23's sealed types and records. The model is designed around the visitor pattern
 * for extensibility without modifying the core types.
 * 
 * <h2>Expression Hierarchy</h2>
 * 
 * <p>The type hierarchy is sealed to enable exhaustive pattern matching:
 * 
 * <pre>
 * Expression (sealed interface)
 * ├── Terminal (sealed interface)
 * │   ├── Token - represents a non-empty symbol from an alphabet
 * │   ├── Empty - represents the empty language (∅)
 * │   └── Epsilon - represents the empty string (ε)
 * └── Composite (sealed interface)
 *     ├── Union - represents choice (A|B)
 *     ├── Concatenation - represents sequencing (A⋅B)
 *     └── KleeneStar - represents repetition (A*)
 * </pre>
 * 
 * <p><b>Important:</b> {@link rege.syntax.model.Token} values must be non-empty.
 * Empty strings should be represented using {@link rege.syntax.model.Epsilon}, not Token.
 * 
 * <h2>Design Patterns</h2>
 * 
 * <h3>Visitor Pattern</h3>
 * <p>All expression types implement {@code accept(Visitor<T,R>, T)} to enable
 * external operations without modifying the sealed hierarchy. The visitor interface
 * is parameterized with:
 * <ul>
 *   <li>{@code T} - the input type for traversal</li>
 *   <li>{@code R} - the return type of the operation</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * public class MyVisitor implements Visitor<Void, Integer> {
 *     public Integer visitToken(Token token, Void input) {
 *         return token.value().length();
 *     }
 *     // ... implement other methods
 * }
 * 
 * Expression expr = new Token("hello");
 * int length = expr.accept(new MyVisitor(), null); // returns 5
 * }</pre>
 * 
 * <h3>Smart Constructors (Factory Pattern)</h3>
 * <p>The {@link rege.syntax.model.Expression} interface provides default methods
 * that implement algebraic laws during construction:
 * <ul>
 *   <li>{@code union(Expression)} - applies identity (∅|A=A) and idempotence (A|A=A)</li>
 *   <li>{@code concat(Expression)} - applies annihilator (∅⋅A=∅) and identity (ε⋅A=A)</li>
 *   <li>{@code star()} - applies ∅*=ε, ε*=ε, (A*)*=A*</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression a = new Token("a");
 * Expression result = a.union(a); // returns a (idempotence)
 * Expression result2 = Expression.EMPTY.concat(a); // returns EMPTY (annihilator)
 * }</pre>
 * 
 * <h3>Singleton Pattern</h3>
 * <p>{@link rege.syntax.model.Empty} and {@link rege.syntax.model.Epsilon} are singletons
 * accessible via constants:
 * <ul>
 *   <li>{@link rege.syntax.model.Expression#EMPTY} - the empty language (∅)</li>
 *   <li>{@link rege.syntax.model.Expression#EPSILON} - the empty string (ε)</li>
 * </ul>
 * 
 * <p>Both singletons are serialization-safe via {@code readResolve()}.
 * 
 * <h2>Algebraic Properties</h2>
 * 
 * <p>The model respects regular expression algebra:
 * 
 * <h3>Union Laws</h3>
 * <ul>
 *   <li>Identity: ∅|A = A|∅ = A</li>
 *   <li>Idempotent: A|A = A</li>
 *   <li>Commutative: A|B = B|A (in {@code equals()}, canonicalized in smart constructor)</li>
 *   <li>Associative: (A|B)|C = A|(B|C) (structure may differ, but semantically equal)</li>
 * </ul>
 * 
 * <h3>Concatenation Laws</h3>
 * <ul>
 *   <li>Annihilator: ∅⋅A = A⋅∅ = ∅</li>
 *   <li>Identity: ε⋅A = A⋅ε = A</li>
 *   <li>Associative: (A⋅B)⋅C = A⋅(B⋅C) (structure may differ, but semantically equal)</li>
 * </ul>
 * 
 * <h3>Kleene Star Laws</h3>
 * <ul>
 *   <li>∅* = ε</li>
 *   <li>ε* = ε</li>
 *   <li>(A*)* = A*</li>
 * </ul>
 * 
 * <h2>Immutability</h2>
 * 
 * <p>All expression types are immutable:
 * <ul>
 *   <li>Records are immutable by default</li>
 *   <li>Singletons have no mutable state</li>
 *   <li>All operations return new instances</li>
 * </ul>
 * 
 * <p>This ensures thread-safety and enables caching (hashCode is stable).
 * 
 * <h2>Equality Semantics</h2>
 * 
 * <p>Equality is <b>structural</b>, not semantic:
 * <ul>
 *   <li>{@code new Token("a").equals(new Token("a"))} is {@code true}</li>
 *   <li>{@code new Union(a,b).equals(new Union(b,a))} is {@code true} (commutative)</li>
 *   <li>{@code new Concatenation(a,b).equals(new Concatenation(b,a))} is {@code false}</li>
 * </ul>
 * 
 * <p>For semantic equivalence, use a visitor that computes canonical forms or
 * checks language equivalence.
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Build expression: (a|b)*⋅c
 * Expression a = new Token("a");
 * Expression b = new Token("b");
 * Expression c = new Token("c");
 * 
 * Expression pattern = a.union(b).star().concat(c);
 * 
 * // Or without smart constructors:
 * Expression pattern2 = new Concatenation(
 *     new KleeneStar(new Union(a, b)),
 *     c
 * );
 * 
 * // Apply a visitor for simplification
 * Expression simplified = pattern.simplify();
 * }</pre>
 * 
 * @see rege.syntax.model.Expression
 * @see rege.syntax.model.Visitor
 */
package rege.syntax.model;
