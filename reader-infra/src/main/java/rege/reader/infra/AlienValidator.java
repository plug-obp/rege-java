package rege.reader.infra;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates content with "alien syntax" - syntax unknown to the host parser.
 * 
 * <p>This interface provides a generic pattern for validating embedded content
 * that uses different syntax rules than the host language. For example:
 * <ul>
 *   <li>Regular expressions containing embedded SQL queries</li>
 *   <li>Parser tokens containing mathematical expressions</li>
 *   <li>Configuration files with embedded JSON/YAML</li>
 *   <li>Any domain-specific language embedded within another</li>
 * </ul>
 * 
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Optional by default</strong>: {@link #acceptAll()} accepts everything</li>
 *   <li><strong>Compositional</strong>: Validators combine with {@link #and(AlienValidator)} and {@link #or(AlienValidator)}</li>
 *   <li><strong>Position-aware</strong>: Errors include precise source ranges</li>
 *   <li><strong>Type-safe</strong>: Uses {@link ParseResult} pattern</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic Validation</h3>
 * <pre>{@code
 * // Accept anything (default)
 * AlienValidator permissive = AlienValidator.acceptAll();
 * 
 * // Reject empty content
 * AlienValidator noEmpty = AlienValidator.nonEmpty();
 * 
 * // Pattern matching
 * AlienValidator lowercase = AlienValidator.pattern(
 *     "[a-z]+",
 *     "Content must be lowercase letters"
 * );
 * }</pre>
 * 
 * <h3>Custom Validators</h3>
 * <pre>{@code
 * // JSON validator
 * AlienValidator jsonValidator = (content, range) -> {
 *     try {
 *         parseJson(content); // your JSON parser
 *         return new ParseResult.Success<>(content);
 *     } catch (Exception e) {
 *         return new ParseResult.Failure<>(
 *             List.of(new ParseError(range, "Invalid JSON: " + e.getMessage())),
 *             content
 *         );
 *     }
 * };
 * 
 * // XML validator
 * AlienValidator xmlValidator = (content, range) -> {
 *     if (!content.matches("<[^>]+>.*</[^>]+>")) {
 *         return new ParseResult.Failure<>(
 *             List.of(new ParseError(range, "Not valid XML")),
 *             content
 *         );
 *     }
 *     return new ParseResult.Success<>(content);
 * };
 * }</pre>
 * 
 * <h3>Composition</h3>
 * <pre>{@code
 * // Both validators must succeed
 * AlienValidator strict = AlienValidator.nonEmpty()
 *     .and(AlienValidator.pattern("[a-zA-Z0-9]+", "Alphanumeric only"))
 *     .and(customValidator);
 * 
 * // Either validator can succeed
 * AlienValidator lenient = jsonValidator.or(xmlValidator);
 * }</pre>
 * 
 * <h2>Implementation Notes</h2>
 * 
 * <p><strong>Position Tracking:</strong> The {@code contentRange} parameter represents
 * the source location of the alien content. Validators should use this range (or
 * create sub-ranges within it) when reporting errors.
 * 
 * <p><strong>Performance:</strong> Validators are called during parsing, so they should
 * be reasonably fast. For expensive validation (e.g., complex parsing), consider:
 * <ul>
 *   <li>Doing lightweight checks during parsing</li>
 *   <li>Deferring deep validation to a separate phase</li>
 *   <li>Caching validation results</li>
 * </ul>
 * 
 * @see ParseResult
 * @see ParseError
 * @see Range
 * @since 2.1
 */
@FunctionalInterface
public interface AlienValidator {
    
    /**
     * Validate alien content.
     * 
     * <p>This method is called during parsing to verify that embedded content
     * conforms to expected syntax. If validation succeeds, return a {@link ParseResult.Success}
     * with the content. If validation fails, return a {@link ParseResult.Failure}
     * with detailed error information.
     * 
     * @param content the alien content to validate (after escape processing)
     * @param contentRange the source range where the content appears
     * @return {@link ParseResult.Success} if valid, {@link ParseResult.Failure} with errors if invalid
     */
    ParseResult<String> validate(String content, Range contentRange);
    
    /**
     * Default validator that accepts all content.
     * 
     * <p>This is the recommended default validator when no validation is needed.
     * It performs no checks and always succeeds.
     * 
     * @return a validator that accepts everything
     */
    static AlienValidator acceptAll() {
        return (content, range) -> new ParseResult.Success<>(content);
    }
    
    /**
     * Validator that rejects empty content.
     * 
     * <p>This validator ensures that alien content is not empty. Use this when
     * the alien syntax requires at least one character.
     * 
     * @return a validator that rejects empty strings
     */
    static AlienValidator nonEmpty() {
        return (content, range) -> {
            if (content.isEmpty()) {
                return new ParseResult.Failure<>(
                    List.of(new ParseError(
                        range,
                        "Content cannot be empty",
                        ParseError.Severity.ERROR,
                        Optional.of("empty-content")
                    )),
                    content
                );
            }
            return new ParseResult.Success<>(content);
        };
    }
    
    /**
     * Validator that checks content matches a regular expression pattern.
     * 
     * <p>This is a convenience method for simple pattern-based validation.
     * For more complex validation logic, implement a custom validator.
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // Only lowercase letters
     * AlienValidator lowercase = AlienValidator.pattern(
     *     "[a-z]+",
     *     "Content must contain only lowercase letters"
     * );
     * 
     * // Alphanumeric with underscores
     * AlienValidator identifier = AlienValidator.pattern(
     *     "[a-zA-Z_][a-zA-Z0-9_]*",
     *     "Content must be a valid identifier"
     * );
     * }</pre>
     * 
     * @param regex the regular expression pattern to match
     * @param errorMessage the error message if pattern doesn't match
     * @return a validator that checks the pattern
     */
    static AlienValidator pattern(String regex, String errorMessage) {
        return (content, range) -> {
            if (!content.matches(regex)) {
                return new ParseResult.Failure<>(
                    List.of(new ParseError(
                        range,
                        errorMessage,
                        ParseError.Severity.ERROR,
                        Optional.of("pattern-mismatch")
                    )),
                    content
                );
            }
            return new ParseResult.Success<>(content);
        };
    }
    
    /**
     * Combine this validator with another validator (both must succeed).
     * 
     * <p>This method creates a new validator that runs both validators in sequence.
     * If the first validator fails, the second is not called. Only if both succeed
     * does the combined validator succeed.
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * AlienValidator strict = AlienValidator.nonEmpty()
     *     .and(AlienValidator.pattern("[a-z]+", "Lowercase only"))
     *     .and(customLengthValidator);
     * }</pre>
     * 
     * @param other the validator to combine with this one
     * @return a new validator that requires both to succeed
     */
    default AlienValidator and(AlienValidator other) {
        return (content, range) -> {
            // Run first validator
            ParseResult<String> result1 = this.validate(content, range);
            if (result1 instanceof ParseResult.Failure<String> failure) {
                return failure;
            }
            
            // First succeeded, run second validator
            return other.validate(content, range);
        };
    }
    
    /**
     * Combine this validator with another validator (either can succeed).
     * 
     * <p>This method creates a new validator that tries both validators. If the first
     * succeeds, the result is returned immediately. If the first fails, the second
     * is tried. If both fail, errors from both validators are combined.
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // Accept either JSON or XML
     * AlienValidator lenient = jsonValidator.or(xmlValidator);
     * }</pre>
     * 
     * @param other the alternative validator
     * @return a new validator that succeeds if either validator succeeds
     */
    default AlienValidator or(AlienValidator other) {
        return (content, range) -> {
            // Try first validator
            ParseResult<String> result1 = this.validate(content, range);
            if (result1 instanceof ParseResult.Success<String> success) {
                return success;
            }
            
            // First failed, try second
            ParseResult<String> result2 = other.validate(content, range);
            if (result2 instanceof ParseResult.Success<String> success) {
                return success;
            }
            
            // Both failed - combine errors
            if (result1 instanceof ParseResult.Failure<String> f1 &&
                result2 instanceof ParseResult.Failure<String> f2) {
                
                List<ParseError> combinedErrors = new ArrayList<>();
                combinedErrors.addAll(f1.errors());
                combinedErrors.addAll(f2.errors());
                return new ParseResult.Failure<>(combinedErrors, content);
            }
            
            // Fallback (shouldn't reach here)
            return result2;
        };
    }
}
