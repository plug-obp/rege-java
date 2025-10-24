package rege.syntax;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import rege.syntax.model.Expression;

/**
 * Represents the result of parsing: either a successful expression or a list of errors.
 * 
 * <p>This sealed interface enforces exhaustive handling via pattern matching:
 * <pre>{@code
 * ParseResult result = RegeReader.parse(input);
 * switch (result) {
 *     case ParseResult.Success(var expr) -> System.out.println("Parsed: " + expr);
 *     case ParseResult.Failure(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * }</pre>
 * 
 * <p>This Result Monad pattern provides type-safe error handling:
 * <ul>
 *   <li>Impossible to use an expression when errors exist</li>
 *   <li>Compiler enforces exhaustive error checking via pattern matching</li>
 *   <li>Composable via {@code map()} and {@code flatMap()}</li>
 *   <li>Interoperable with exceptions via {@code orElseThrow()}</li>
 * </ul>
 */
public sealed interface ParseResult {
    
    /**
     * Represents a successful parse result containing an expression.
     * 
     * @param expression the successfully parsed expression (never null)
     */
    record Success(Expression expression) implements ParseResult {
        public Success {
            Objects.requireNonNull(expression, "expression cannot be null");
        }
    }
    
    /**
     * Represents a failed parse result containing errors and the source text.
     * 
     * @param errors the list of parse errors (never empty)
     * @param source the source text that failed to parse
     */
    record Failure(List<ParseError> errors, String source) implements ParseResult {
        public Failure {
            Objects.requireNonNull(errors, "errors cannot be null");
            Objects.requireNonNull(source, "source cannot be null");
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("errors cannot be empty");
            }
        }
    }
    
    /**
     * Check if this is a successful parse result.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    /**
     * Check if this is a failed parse result.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }
    
    /**
     * Get the expression if successful, or throw a ParseException if failed.
     * 
     * @return the parsed expression
     * @throws ParseException if this is a Failure
     */
    default Expression orElseThrow() throws ParseException {
        return switch (this) {
            case Success(var expr) -> expr;
            case Failure(var errors, var source) -> throw new ParseException(errors, source);
        };
    }
    
    /**
     * Get the expression if successful, or return an empty Optional if failed.
     */
    default Optional<Expression> toOptional() {
        return switch (this) {
            case Success(var expr) -> Optional.of(expr);
            case Failure(var errors, var source) -> Optional.empty();
        };
    }
    
    /**
     * Map the expression if successful, or return the same failure.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ParseResult result = RegeReader.parse(input);
     * ParseResult simplified = result.map(Expression::simplify);
     * }</pre>
     */
    default ParseResult map(Function<Expression, Expression> f) {
        Objects.requireNonNull(f, "mapping function cannot be null");
        return switch (this) {
            case Success(var expr) -> new Success(f.apply(expr));
            case Failure failure -> failure;
        };
    }
    
    /**
     * FlatMap the expression if successful, or return the same failure.
     * Useful for chaining parse operations.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ParseResult result = RegeReader.parse(input)
     *     .flatMap(expr -> validateSemantics(expr));
     * }</pre>
     */
    default ParseResult flatMap(Function<Expression, ParseResult> f) {
        Objects.requireNonNull(f, "mapping function cannot be null");
        return switch (this) {
            case Success(var expr) -> f.apply(expr);
            case Failure failure -> failure;
        };
    }
    
    /**
     * Get the expression if successful, or return a default value if failed.
     */
    default Expression orElse(Expression defaultValue) {
        return switch (this) {
            case Success(var expr) -> expr;
            case Failure(var errors, var source) -> defaultValue;
        };
    }
    
    /**
     * Format all errors with source context if this is a failure.
     * Returns empty string if this is a success.
     */
    default String formatErrors() {
        return switch (this) {
            case Success success -> "";
            case Failure(var errors, var source) -> {
                StringBuilder sb = new StringBuilder();
                for (ParseError error : errors) {
                    sb.append(error.formatWithSource(source));
                    sb.append("\n");
                }
                yield sb.toString();
            }
        };
    }
}
