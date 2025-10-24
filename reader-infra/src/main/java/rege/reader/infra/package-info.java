/**
 * Reader infrastructure providing reusable parsing utilities for type-safe error handling.
 * 
 * <p>This package provides a complete Result Monad pattern implementation for parsers,
 * with position tracking, detailed error messages, and LSP (Language Server Protocol) compatibility.
 * It's designed to be parser-agnostic and can be used by any text parsing project.
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>ParseResult - Result Monad</h3>
 * <p>{@link rege.reader.infra.ParseResult} is a generic sealed interface implementing
 * the Result Monad pattern for type-safe error handling:
 * 
 * <pre>{@code
 * ParseResult<Expression> result = parser.parse(input);
 * switch (result) {
 *     case ParseResult.Success<Expression>(var expr) -> 
 *         System.out.println("Success: " + expr);
 *     case ParseResult.Failure<Expression>(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * }</pre>
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Sealed interface enforces exhaustive pattern matching</li>
 *   <li>Generic type parameter for any parsed value type</li>
 *   <li>Composable via {@code map()} and {@code flatMap()}</li>
 *   <li>Interoperable with exceptions via {@code orElseThrow()}</li>
 *   <li>Optional-based access via {@code toOptional()}</li>
 * </ul>
 * 
 * <h3>Position Tracking</h3>
 * <p>{@link rege.reader.infra.Position} tracks locations in source text with three coordinates:
 * <ul>
 *   <li><b>line</b> - 1-based line number (human-readable, LSP compatible)</li>
 *   <li><b>column</b> - 1-based column number (human-readable, LSP compatible)</li>
 *   <li><b>offset</b> - 0-based character offset (programmatic access)</li>
 * </ul>
 * 
 * <pre>{@code
 * Position pos = new Position(1, 5, 4);  // Line 1, column 5, offset 4
 * Position start = Position.start();      // (1, 1, 0)
 * }</pre>
 * 
 * <h3>Range Representation</h3>
 * <p>{@link rege.reader.infra.Range} represents text spans from start to end position:
 * 
 * <pre>{@code
 * Range span = new Range(start, end);
 * Range point = Range.at(position);  // Zero-width range
 * 
 * if (span.contains(position)) {
 *     int length = span.length();
 * }
 * }</pre>
 * 
 * <h3>Error Reporting</h3>
 * <p>{@link rege.reader.infra.ParseError} provides detailed error information:
 * 
 * <pre>{@code
 * ParseError error = new ParseError(
 *     range,
 *     "Unexpected character '}'",
 *     ParseError.Severity.ERROR,
 *     Optional.of("unexpected-char")
 * );
 * 
 * // Format with source context
 * String formatted = error.formatWithSource(source);
 * // Output:
 * // error at 1:5-1:6: Unexpected character '}'
 * //   let x = }
 * //           ^
 * }</pre>
 * 
 * <p><b>Severity Levels</b> (LSP DiagnosticSeverity compatible):
 * <ul>
 *   <li>{@code ERROR} - Prevents parsing (severity 1)</li>
 *   <li>{@code WARNING} - Doesn't prevent parsing (severity 2)</li>
 *   <li>{@code INFO} - Informational message (severity 3)</li>
 *   <li>{@code HINT} - Suggestion (severity 4)</li>
 * </ul>
 * 
 * <h3>Exception Handling</h3>
 * <p>{@link rege.reader.infra.ParseException} bridges Result Monad and exception-based error handling:
 * 
 * <pre>{@code
 * try {
 *     T result = parseResult.orElseThrow();
 * } catch (ParseException e) {
 *     System.err.println(e.getMessage());  // Formatted multi-error message
 *     e.getErrors().forEach(error -> processError(error));
 * }
 * }</pre>
 * 
 * <h3>Peekable Iterator</h3>
 * <p>{@link rege.reader.infra.Peekable} provides character-by-character parsing with lookahead:
 * 
 * <pre>{@code
 * Peekable input = new Peekable("hello");
 * Position start = input.position();
 * 
 * if (input.hasNext() && input.peek() == 'h') {
 *     char ch = input.next();  // Consume 'h'
 *     Range range = input.rangeFrom(start);
 * }
 * }</pre>
 * 
 * <p><b>Features:</b>
 * <ul>
 *   <li>One-character lookahead via {@code peek()}</li>
 *   <li>Automatic position tracking (line, column, offset)</li>
 *   <li>Newline handling (increments line, resets column)</li>
 *   <li>Range creation helpers: {@code rangeFrom()}, {@code rangeHere()}</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Pattern Matching (Recommended)</h3>
 * <pre>{@code
 * ParseResult<MyType> result = myParser.parse(input);
 * switch (result) {
 *     case ParseResult.Success<MyType>(var value) -> {
 *         // Process successful result
 *         processValue(value);
 *     }
 *     case ParseResult.Failure<MyType>(var errors, var source) -> {
 *         // Handle all errors
 *         for (ParseError error : errors) {
 *             System.err.println(error.formatWithSource(source));
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h3>Functional Composition</h3>
 * <pre>{@code
 * ParseResult<ProcessedType> result = parser.parse(input)
 *     .map(value -> transform(value))
 *     .flatMap(value -> validate(value))
 *     .map(value -> finalize(value));
 * }</pre>
 * 
 * <h3>Exception-Based (Legacy Integration)</h3>
 * <pre>{@code
 * try {
 *     MyType value = parser.parse(input).orElseThrow();
 *     return processValue(value);
 * } catch (ParseException e) {
 *     logger.error("Parse failed: {}", e.getMessage());
 *     throw new ApplicationException(e);
 * }
 * }</pre>
 * 
 * <h3>Optional-Based</h3>
 * <pre>{@code
 * Optional<MyType> opt = parser.parse(input).toOptional();
 * opt.ifPresentOrElse(
 *     value -> System.out.println("Success: " + value),
 *     () -> System.err.println("Parse failed")
 * );
 * }</pre>
 * 
 * <h2>Building a Parser</h2>
 * 
 * <p>Example of building a simple expression parser using this infrastructure:
 * 
 * <pre>{@code
 * public class MyParser {
 *     private final List<ParseError> errors = new ArrayList<>();
 *     private final Peekable input;
 *     
 *     public static ParseResult<Expression> parse(String source) {
 *         MyParser parser = new MyParser(source);
 *         Expression result = parser.parseExpression();
 *         
 *         if (!parser.errors.isEmpty()) {
 *             return new ParseResult.Failure<>(parser.errors, source);
 *         }
 *         if (result == null) {
 *             parser.error(parser.input.rangeHere(), "Failed to parse expression");
 *             return new ParseResult.Failure<>(parser.errors, source);
 *         }
 *         return new ParseResult.Success<>(result);
 *     }
 *     
 *     private MyParser(String source) {
 *         this.input = new Peekable(source);
 *     }
 *     
 *     private void error(Range range, String message) {
 *         errors.add(new ParseError(range, message));
 *     }
 *     
 *     private Expression parseExpression() {
 *         Position start = input.position();
 *         
 *         if (!input.hasNext()) {
 *             error(input.rangeHere(), "Unexpected end of input");
 *             return null;
 *         }
 *         
 *         // Parsing logic...
 *         
 *         return result;
 *     }
 * }
 * }</pre>
 * 
 * <h2>LSP Integration</h2>
 * 
 * <p>This infrastructure is designed for Language Server Protocol compatibility:
 * 
 * <pre>{@code
 * // Convert ParseError to LSP Diagnostic
 * Diagnostic toDiagnostic(ParseError error) {
 *     return new Diagnostic(
 *         toLspRange(error.range()),
 *         error.message(),
 *         DiagnosticSeverity.forValue(error.severity().getLspValue()),
 *         "my-parser",
 *         error.code().orElse(null)
 *     );
 * }
 * 
 * org.eclipse.lsp4j.Range toLspRange(Range range) {
 *     return new org.eclipse.lsp4j.Range(
 *         new org.eclipse.lsp4j.Position(
 *             range.start().line() - 1,  // LSP is 0-based
 *             range.start().column() - 1
 *         ),
 *         new org.eclipse.lsp4j.Position(
 *             range.end().line() - 1,
 *             range.end().column() - 1
 *         )
 *     );
 * }
 * }</pre>
 * 
 * <h2>Design Principles</h2>
 * 
 * <ul>
 *   <li><b>Type Safety</b> - Sealed interfaces and pattern matching eliminate null checks</li>
 *   <li><b>Immutability</b> - All data structures are immutable records</li>
 *   <li><b>Composability</b> - Functional methods enable clean error handling chains</li>
 *   <li><b>LSP Compatible</b> - Structures map directly to LSP types</li>
 *   <li><b>Zero Dependencies</b> - Pure Java 23, no external dependencies</li>
 *   <li><b>Generic</b> - Works with any parsed value type</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * 
 * <ul>
 *   <li>Peekable uses direct string indexing (O(1) access)</li>
 *   <li>Position tracking adds minimal overhead per character</li>
 *   <li>Error collection uses ArrayList (amortized O(1) append)</li>
 *   <li>Immutable collections prevent accidental mutations</li>
 *   <li>Records provide efficient memory layout</li>
 * </ul>
 * 
 * @see rege.reader.infra.ParseResult
 * @see rege.reader.infra.Position
 * @see rege.reader.infra.Range
 * @see rege.reader.infra.ParseError
 * @see rege.reader.infra.ParseException
 * @see rege.reader.infra.Peekable
 */
package rege.reader.infra;
