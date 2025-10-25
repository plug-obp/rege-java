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
 * ParseResult result = RegeReader.parse("τ[a]τ[b]τ[c]");
 * switch (result) {
 *     case ParseResult.Success(var expr) -> {
 *         // Produces: Concatenation(Token("a"), Concatenation(Token("b"), Token("c")))
 *         // Structure: a⋅(b⋅c)  [right-associative]
 *     }
 *     case ParseResult.Failure(var errors, var source) -> {
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 *     }
 * }
 * }</pre>
 * 
 * <h3>RegeReaderLeft (Left-Associative)</h3>
 * <p>{@link rege.syntax.RegeReaderLeft} implements an iterative parser that
 * produces left-associative parse trees.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * ParseResult result = RegeReaderLeft.parse("τ[a]τ[b]τ[c]");
 * switch (result) {
 *     case ParseResult.Success(var expr) -> {
 *         // Produces: Concatenation(Concatenation(Token("a"), Token("b")), Token("c"))
 *         // Structure: (a⋅b)⋅c  [left-associative]
 *     }
 *     case ParseResult.Failure(var errors, var source) -> {
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 *     }
 * }
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
 *   <caption>Token Escape Sequences</caption>
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
 * ParseResult result1 = RegeReader.parse("ε|ε");
 * // Success with: Epsilon (simplified via A|A = A)
 * 
 * // Raw mode (no simplification)
 * ParseResult result2 = RegeReader.parse("ε|ε", false);
 * // Success with: Union(Epsilon, Epsilon)
 * }</pre>
 * 
 * <h2>Error Handling</h2>
 * 
 * <p>Both parsers return {@link rege.reader.infra.ParseResult}{@code <Expression>},
 * providing type-safe error handling with precise position tracking.
 * 
 * <p><b>Basic Usage:</b>
 * <pre>{@code
 * ParseResult<Expression> result = RegeReader.parse("τ[a]|τ[b]");
 * switch (result) {
 *     case ParseResult.Success<Expression>(var expr) -> 
 *         System.out.println("Parsed: " + expr);
 *     case ParseResult.Failure<Expression>(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * }</pre>
 * 
 * <p>For complete error handling documentation, including:
 * <ul>
 *   <li>Result Monad pattern with {@link rege.reader.infra.ParseResult}</li>
 *   <li>Position tracking with {@link rege.reader.infra.Position} and {@link rege.reader.infra.Range}</li>
 *   <li>Error reporting with {@link rege.reader.infra.ParseError}</li>
 *   <li>Exception bridge with {@link rege.reader.infra.ParseException}</li>
 *   <li>Functional composition (map, flatMap)</li>
 *   <li>LSP (Language Server Protocol) integration</li>
 * </ul>
 * 
 * <p><b>See:</b> {@link rege.reader.infra} package documentation
 * 
 * <h3>Token Content Validation</h3>
 * <p>Both parsers support validating token content using {@link rege.reader.infra.AlienValidator}.
 * This is useful when tokens contain domain-specific syntax that needs validation:
 * 
 * <pre>{@code
 * // Only allow lowercase letters in tokens
 * AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase only");
 * ParseResult<Expression> result = RegeReader.parse("τ[hello]|τ[WORLD]", true, lowercase);
 * // WORLD will cause a validation error
 * 
 * // Compose validators
 * AlienValidator strict = AlienValidator.nonEmpty()
 *     .and(AlienValidator.pattern("[a-zA-Z0-9]+", "Alphanumeric"))
 *     .and(customValidator);
 * 
 * // Custom validation
 * AlienValidator balanced = (content, range) -> {
 *     // Check for balanced parentheses in token content
 *     if (!isBalanced(content)) {
 *         return new ParseResult.Failure<>(
 *             List.of(new ParseError(range, "Unbalanced parentheses")),
 *             content
 *         );
 *     }
 *     return new ParseResult.Success<>(content);
 * };
 * }</pre>
 * 
 * <p><b>See:</b> {@link rege.reader.infra.AlienValidator} for complete documentation
 * 
 * <h3>Backward Compatibility</h3>
 * <p>Legacy methods are preserved but deprecated:
 * <pre>{@code
 * // Old API (deprecated, returns null on error)
 * Expression expr = RegeReader.readExpression("τ[a]");
 * 
 * // New API (recommended, returns ParseResult<Expression>)
 * ParseResult<Expression> result = RegeReader.parse("τ[a]");
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
 * <p>The {@code Simplifier} visitor recursively applies
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
 * @see rege.syntax.RegeReader
 * @see rege.syntax.RegeReaderLeft
 * @see rege.syntax.PrettyPrinter
 * @see rege.syntax.model
 * @see rege.reader.infra
 */
package rege.syntax;
