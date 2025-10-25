package rege.reader.infra;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents the result of parsing: either a successful value or a list of errors.
 * 
 * <p>This sealed interface enforces exhaustive handling via pattern matching:
 * <pre>{@code
 * ParseResult<Expression> result = parser.parse(input);
 * switch (result) {
 *     case ParseResult.Success<Expression>(var expr) -> 
 *         System.out.println("Parsed: " + expr);
 *     case ParseResult.Failure<Expression>(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * }</pre>
 * 
 * <p>This Result Monad pattern provides type-safe error handling:
 * <ul>
 *   <li>Impossible to use a value when errors exist</li>
 *   <li>Compiler enforces exhaustive error checking via pattern matching</li>
 *   <li>Composable via {@code map()} and {@code flatMap()}</li>
 *   <li>Interoperable with exceptions via {@code orElseThrow()}</li>
 * </ul>
 * 
 * @param <T> the type of the successfully parsed value
 */
public sealed interface ParseResult<T> {
    
    /**
     * Represents a successful parse result containing a value.
     * 
     * @param value the successfully parsed value (never null)
     * @param <T> the type of the value
     */
    record Success<T>(T value) implements ParseResult<T> {
        /**
         * Compact constructor that validates the value is non-null.
         */
        public Success {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }
    
    /**
     * Represents a failed parse result containing errors and the source text.
     * 
     * @param errors the list of parse errors (never empty)
     * @param source the source text that failed to parse
     * @param <T> the type that would have been returned on success
     */
    record Failure<T>(List<ParseError> errors, String source) implements ParseResult<T> {
        /**
         * Compact constructor that validates errors is non-null and non-empty.
         */
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
     * @return true if this is a Success, false otherwise
     */
    default boolean isSuccess() {
        return this instanceof Success<T>;
    }
    
    /**
     * Check if this is a failed parse result.
     * @return true if this is a Failure, false otherwise
     */
    default boolean isFailure() {
        return this instanceof Failure<T>;
    }
    
    /**
     * Get the value if successful, or throw a ParseException if failed.
     * 
     * @return the parsed value
     * @throws ParseException if this is a Failure
     */
    default T orElseThrow() throws ParseException {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T>(var errors, var source) -> throw new ParseException(errors, source);
        };
    }
    
    /**
     * Get the value if successful, or return an empty Optional if failed.
     * @return an Optional containing the value if successful, empty otherwise
     */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T>(var value) -> Optional.of(value);
            case Failure<T>(var errors, var source) -> Optional.empty();
        };
    }
    
    /**
     * Map the value if successful, or return the same failure.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ParseResult<Expression> result = parser.parse(input);
     * ParseResult<Expression> simplified = result.map(Expression::simplify);
     * }</pre>
     * 
     * @param f the mapping function
     * @param <U> the type of the mapped value
     * @return a new ParseResult with the mapped value, or the same failure
     */
    default <U> ParseResult<U> map(Function<T, U> f) {
        Objects.requireNonNull(f, "mapping function cannot be null");
        return switch (this) {
            case Success<T>(var value) -> new Success<>(f.apply(value));
            case Failure<T>(var errors, var source) -> new Failure<>(errors, source);
        };
    }
    
    /**
     * FlatMap the value if successful, or return the same failure.
     * Useful for chaining parse operations.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ParseResult<Expression> result = parser.parse(input)
     *     .flatMap(expr -> validateSemantics(expr));
     * }</pre>
     * 
     * @param f the mapping function that returns a ParseResult
     * @param <U> the type of the mapped value
     * @return the result of applying f, or the same failure
     */
    default <U> ParseResult<U> flatMap(Function<T, ParseResult<U>> f) {
        Objects.requireNonNull(f, "mapping function cannot be null");
        return switch (this) {
            case Success<T>(var value) -> f.apply(value);
            case Failure<T>(var errors, var source) -> new Failure<>(errors, source);
        };
    }
    
    /**
     * Get the value if successful, or return a default value if failed.
     * 
     * @param defaultValue the value to return if this is a failure
     * @return the parsed value or the default value
     */
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T>(var errors, var source) -> defaultValue;
        };
    }
    
    /**
     * Format all errors with source context if this is a failure.
     * Returns empty string if this is a success.
     * 
     * @return formatted error messages, or empty string
     */
    default String formatErrors() {
        return switch (this) {
            case Success<T> success -> "";
            case Failure<T>(var errors, var source) -> {
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
