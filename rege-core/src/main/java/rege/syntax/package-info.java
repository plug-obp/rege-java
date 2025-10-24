/**
 * Parsers and utilities for regular expression syntax.
 * 
 * <p>This package provides tools for converting between textual representations
 * and the expression model defined in {@link rege.syntax.model}.
 * 
 * <h2>Parsers</h2>
 * 
 * <p>Both parsers implement <b>standard regex precedence</b> (star &gt; concat &gt; union)
 * but differ in <b>associativity</b>.
 * 
 * <h3>RegeReader (Right-Associative)</h3>
 * <p>{@link rege.syntax.RegeReader} implements a recursive descent parser that
 * produces right-associative parse trees. This is the default parser.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression expr = RegeReader.readExpression("τ[a]τ[b]τ[c]");
 * // Produces: Concatenation(Token("a"), Concatenation(Token("b"), Token("c")))
 * // Structure: a⋅(b⋅c)  [right-associative]
 * }</pre>
 * 
 * <h3>RegeReaderLeft (Left-Associative)</h3>
 * <p>{@link rege.syntax.RegeReaderLeft} implements an iterative parser that
 * produces left-associative parse trees.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression expr = RegeReaderLeft.readExpression("τ[a]τ[b]τ[c]");
 * // Produces: Concatenation(Concatenation(Token("a"), Token("b")), Token("c"))
 * // Structure: (a⋅b)⋅c  [left-associative]
 * }</pre>
 * 
 * <h3>Grammar</h3>
 * <p>Both parsers support the same grammar with Unicode mathematical symbols:
 * 
 * <pre>
 * E → ∅              (empty language, U+2205)
 *   | ε              (epsilon, U+03B5)
 *   | τ[value]       (token with value)
 *   | t[value]       (alternative token syntax)
 *   | (E)            (parenthesized expression)
 *   | E | E          (union, can also use ∪)
 *   | E . E          (concatenation, can also use ⋅ or implicit)
 *   | E *            (Kleene star)
 * </pre>
 * 
 * <h3>Operator Precedence (Standard Regex)</h3>
 * <p>Both parsers implement the same precedence (highest to lowest):
 * <ol>
 *   <li><b>Kleene star (*)</b> - highest precedence, binds tightest</li>
 *   <li><b>Concatenation (⋅ or implicit)</b> - middle precedence</li>
 *   <li><b>Union (|)</b> - lowest precedence, binds loosest</li>
 * </ol>
 * 
 * <p><b>Precedence Examples:</b>
 * <ul>
 *   <li>{@code a|b*} parses as {@code a|(b*)} - star binds before union</li>
 *   <li>{@code ab*} parses as {@code a(b*)} - star binds before concat</li>
 *   <li>{@code a|bc} parses as {@code a|(bc)} - concat binds before union</li>
 *   <li>{@code a.b|c.d} parses as {@code (a.b)|(c.d)} - both concats before union</li>
 *   <li>{@code a*b|c} parses as {@code ((a*)b)|c} - star, then concat, then union</li>
 * </ul>
 * 
 * <h3>Escape Sequences</h3>
 * <p>Token values support escape sequences for special characters. The parsers
 * process these escapes, so token values contain the <i>interpreted</i> characters:
 * 
 * <table border="1">
 *   <tr><th>Escape</th><th>Character</th><th>Example</th></tr>
 *   <tr><td>\n</td><td>Newline</td><td>{@code τ[hello\nworld]} → Token("hello\nworld")</td></tr>
 *   <tr><td>\t</td><td>Tab</td><td>{@code τ[a\tb]} → Token("a\tb")</td></tr>
 *   <tr><td>\\</td><td>Backslash</td><td>{@code τ[a\\b]} → Token("a\b")</td></tr>
 *   <tr><td>\]</td><td>Closing bracket</td><td>{@code τ[a\]b]} → Token("a]b")</td></tr>
 *   <tr><td>\[</td><td>Opening bracket</td><td>{@code τ[a\[b]} → Token("a[b")</td></tr>
 * </table>
 * 
 * <p><b>Important:</b> Token values are interpreted, not literal:
 * <ul>
 *   <li>Parser input: {@code "τ[hello\nworld]"} contains escape syntax</li>
 *   <li>Token value: {@code "hello\nworld"} contains actual newline character</li>
 *   <li>External tools receive the interpreted string with real special characters</li>
 * </ul>
 * 
 * <h3>Smart Construction</h3>
 * <p>Both parsers support smart construction mode (enabled by default), which
 * applies algebraic simplifications during parsing:
 * 
 * <pre>{@code
 * // Smart mode (default)
 * Expression expr1 = RegeReader.readExpression("ε|ε");
 * // Returns: Epsilon (simplified via A|A = A)
 * 
 * // Raw mode (no simplification)
 * Expression expr2 = RegeReader.readExpression("ε|ε", false);
 * // Returns: Union(Epsilon, Epsilon)
 * }</pre>
 * 
 * <h2>Pretty Printer</h2>
 * 
 * <h3>PrettyPrinter</h3>
 * <p>{@link rege.syntax.PrettyPrinter} converts expression trees back to text
 * using the same syntax accepted by the parsers.
 * 
 * <p><b>Features:</b>
 * <ul>
 *   <li>Produces minimal parentheses based on precedence</li>
 *   <li>Supports both implicit and explicit concatenation operators</li>
 *   <li>Escapes special characters in token values</li>
 *   <li>Guarantees roundtrip property: {@code parse(print(expr)).equals(expr)}</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Expression expr = new Concatenation(
 *     new KleeneStar(new Union(new Token("a"), new Token("b"))),
 *     new Token("c")
 * );
 * 
 * String text = PrettyPrinter.print(expr);
 * // Returns: "(τ[a]|τ[b])*τ[c]"
 * 
 * Expression parsed = RegeReader.read(text);
 * assert expr.equals(parsed); // true
 * }</pre>
 * 
 * <h3>Explicit Concatenation</h3>
 * <p>The pretty printer can optionally include the concatenation operator:
 * 
 * <pre>{@code
 * Expression expr = new Concatenation(new Token("a"), new Token("b"));
 * 
 * String implicit = PrettyPrinter.print(expr);
 * // Returns: "τ[a]τ[b]"
 * 
 * String explicit = PrettyPrinter.print(expr, true);
 * // Returns: "τ[a]⋅τ[b]"
 * }</pre>
 * 
 * <h2>Simplifier</h2>
 * 
 * <h3>Algebraic Simplification</h3>
 * <p>{@link rege.syntax.Simplifier} is a visitor that recursively applies
 * algebraic laws to simplify expressions:
 * 
 * <pre>{@code
 * Expression expr = new Union(
 *     new Token("a"),
 *     new Union(new Token("a"), new Token("b"))
 * );
 * 
 * Expression simplified = Simplifier.simplify(expr);
 * // Returns: Union(Token("a"), Token("b"))
 * // Applied: A|(A|B) = A|B (absorption)
 * }</pre>
 * 
 * <p><b>Implemented Laws:</b>
 * <ul>
 *   <li><b>Union:</b> ∅|A=A, A|A=A, A|(A|B)=A|B (absorption)</li>
 *   <li><b>Concatenation:</b> ∅⋅A=∅, ε⋅A=A</li>
 *   <li><b>Kleene Star:</b> ∅*=ε, ε*=ε, (A*)*=A*</li>
 * </ul>
 * 
 * <h2>Roundtripping</h2>
 * 
 * <p>The design guarantees that parsing and printing are inverses (up to associativity):
 * 
 * <pre>{@code
 * // Roundtrip with RegeReader (right-associative)
 * Expression original = new Concatenation(
 *     new Token("a"),
 *     new Concatenation(new Token("b"), new Token("c"))
 * );
 * String printed = PrettyPrinter.print(original);
 * Expression parsed = RegeReader.read(printed);
 * assert original.equals(parsed); // true
 * 
 * // Note: Left-associative structures may change associativity
 * Expression leftAssoc = new Concatenation(
 *     new Concatenation(new Token("a"), new Token("b")),
 *     new Token("c")
 * );
 * String printed2 = PrettyPrinter.print(leftAssoc);
 * Expression parsed2 = RegeReader.read(printed2);
 * // parsed2 is right-associative: a⋅(b⋅c)
 * // Not equal structurally, but semantically equivalent
 * }</pre>
 * 
 * <h2>Character Encoding</h2>
 * 
 * <p>All tools use UTF-8 and support Unicode mathematical symbols:
 * <ul>
 *   <li>∅ (U+2205, EMPTY SET)</li>
 *   <li>ε (U+03B5, GREEK SMALL LETTER EPSILON)</li>
 *   <li>τ (U+03C4, GREEK SMALL LETTER TAU)</li>
 *   <li>⋅ (U+22C5, DOT OPERATOR)</li>
 *   <li>∪ (U+222A, UNION) - alternative to |</li>
 * </ul>
 * 
 * <h2>Utilities</h2>
 * 
 * <h3>Peekable</h3>
 * <p>{@link rege.syntax.Peekable} is a lightweight character iterator with
 * lookahead capability. Both parsers use this utility to implement one-character
 * lookahead parsing without backtracking.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * Peekable input = new Peekable("abc");
 * if (input.hasNext() && input.peek() == 'a') {
 *     char ch = input.next(); // consume 'a'
 *     // now input is positioned at 'b'
 * }
 * }</pre>
 * 
 * @see rege.syntax.RegeReader
 * @see rege.syntax.RegeReaderLeft
 * @see rege.syntax.PrettyPrinter
 * @see rege.syntax.Simplifier
 * @see rege.syntax.Peekable
 * @see rege.syntax.model
 */
package rege.syntax;
