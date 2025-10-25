package rege.syntax;

import rege.reader.infra.*;
import rege.syntax.model.*;

/**
 * Parser for regular expressions that produces RIGHT-ASSOCIATIVE parse trees.
 * <p>
 * This parser implements standard regex precedence (highest to lowest):
 * <ol>
 *   <li>Kleene star (*) - highest precedence</li>
 *   <li>Concatenation (⋅) - middle precedence</li>
 *   <li>Union (|) - lowest precedence</li>
 * </ol>
 * 
 * <p>Operators are RIGHT-ASSOCIATIVE:
 * <ul>
 *   <li>a.b.c → a⋅(b⋅c)</li>
 *   <li>a|b|c → a|(b|c)</li>
 *   <li>a** → (a*)*</li>
 * </ul>
 * 
 * <p>Grammar (recursive descent with precedence):
 * <pre>
 * E  -> T E'                      // Union (lowest precedence)
 * E' -> | T E' | ∪ T E' | ε
 * 
 * T  -> F T'                      // Concatenation (middle precedence)
 * T' -> . F T' | ⋅ F T' | F T' | ε (implicit concat)
 * 
 * F  -> P F'                      // Kleene star (highest precedence)
 * F' -> * F' | ε
 * 
 * P  -> ∅ | ϵ | τ[...] | (E)      // Primary (atoms)
 * </pre>
 * 
 * <p>Returns {@link ParseResult} with either a successfully parsed expression
 * or a list of errors with precise position information.
 * 
 * <p>Usage:
 * <pre>{@code
 * ParseResult result = RegeReader.parse("τ[a] | τ[b]");
 * switch (result) {
 *     case ParseResult.Success(var expr) -> System.out.println("Parsed: " + expr);
 *     case ParseResult.Failure(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * 
 * // Or use orElseThrow for exception-based error handling:
 * try {
 *     Expression expr = RegeReader.parse("τ[a] | τ[b]").orElseThrow();
 * } catch (ParseException e) {
 *     e.getErrors().forEach(err -> System.err.println(err));
 * }
 * }</pre>
 */
public class RegeReader {
    
    private final boolean isSmart;
    private final AlienValidator alienValidator;
    private final java.util.List<ParseError> errors = new java.util.ArrayList<>();
    
    /**
     * Create a reader with specified smartness and alien validator.
     * 
     * @param isSmart if true, uses smart constructors (union(), concat(), star())
     *                which apply simplification rules
     * @param alienValidator validator for token content (alien syntax)
     */
    private RegeReader(boolean isSmart, AlienValidator alienValidator) {
        this.isSmart = isSmart;
        this.alienValidator = alienValidator;
    }
    
    /**
     * Parse an expression from string with smart constructors and no token validation.
     * 
     * @param input the input string
     * @return parse result containing either the expression or errors
     */
    public static ParseResult<Expression> parse(String input) {
        return parse(input, true, AlienValidator.acceptAll());
    }
    
    /**
     * Parse an expression from string with specified smartness and no token validation.
     * 
     * @param input the input string
     * @param isSmart if true, uses smart constructors
     * @return parse result containing either the expression or errors
     */
    public static ParseResult<Expression> parse(String input, boolean isSmart) {
        return parse(input, isSmart, AlienValidator.acceptAll());
    }
    
    /**
     * Parse an expression from string with token validation.
     * 
     * <p>The alien validator is called for each token's content to verify it conforms
     * to expected syntax. For example, if tokens should contain only lowercase letters:
     * <pre>{@code
     * AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase only");
     * ParseResult<Expression> result = RegeReader.parse("τ[hello]|τ[WORLD]", true, lowercase);
     * // WORLD will cause a validation error
     * }</pre>
     * 
     * @param input the input string
     * @param isSmart if true, uses smart constructors
     * @param alienValidator validator for token content
     * @return parse result containing either the expression or errors
     */
    public static ParseResult<Expression> parse(String input, boolean isSmart, AlienValidator alienValidator) {
        RegeReader reader = new RegeReader(isSmart, alienValidator);
        Peekable peekable = new Peekable(input);
        Expression expr = reader.parseUnion(peekable);
        
        if (!reader.errors.isEmpty()) {
            return new ParseResult.Failure<>(reader.errors, input);
        }
        
        if (expr == null) {
            // No errors but failed to parse - generic error
            reader.error(peekable.rangeHere(), "Failed to parse expression");
            return new ParseResult.Failure<>(reader.errors, input);
        }
        
        // Check for trailing characters
        reader.eatSpace(peekable);
        if (peekable.hasNext()) {
            Position start = peekable.position();
            // Consume all trailing characters to show full range
            while (peekable.hasNext()) {
                peekable.next();
            }
            reader.error(peekable.rangeFrom(start), "Unexpected trailing characters");
            return new ParseResult.Failure<>(reader.errors, input);
        }
        
        return new ParseResult.Success<>(expr);
    }
    
    /**
     * Record an error at the specified range.
     */
    private void error(Range range, String message) {
        errors.add(new ParseError(range, message));
    }
    
    /**
     * Parse a union expression (lowest precedence).
     * <p>
     * Grammar: E -> T ('|'|'∪' T)?
     * <p>
     * Right-associative: a|b|c → a|(b|c)
     */
    private Expression parseUnion(Peekable input) {
        Expression left = parseConcatenation(input);
        if (left == null) {
            return null;
        }
        
        eatSpace(input);
        if (!input.hasNext()) {
            return left;
        }
        
        char ch = input.peek();
        if (ch != '|' && ch != '∪') {
            return left;
        }
        
        Position opPos = input.position();
        input.next(); // consume operator
        
        // Right-recursive: parse rest as another union
        Expression right = parseUnion(input);
        if (right == null) {
            error(input.rangeFrom(opPos), "Expected expression after union operator '|'");
            return null;
        }
        
        if (isSmart) {
            return left.union(right);
        }
        return new Union(left, right);
    }
    
    /**
     * Parse a concatenation expression (middle precedence).
     * <p>
     * Grammar: T -> F ('.'|'⋅' F | F)?
     * <p>
     * Right-associative: a.b.c → a⋅(b⋅c)
     */
    private Expression parseConcatenation(Peekable input) {
        Expression left = parseKleene(input);
        if (left == null) {
            return null;
        }
        
        eatSpace(input);
        if (!input.hasNext()) {
            return left;
        }
        
        char ch = input.peek();
        
        // Check for explicit concatenation operators
        boolean hasExplicitOperator = (ch == '.' || ch == '⋅');
        Position opPos = null;
        if (hasExplicitOperator) {
            opPos = input.position();
            input.next(); // consume operator
            eatSpace(input);
        }
        
        // Stop if we see a union or closing paren (lower precedence or end of group)
        if (ch == '|' || ch == '∪' || ch == ')') {
            return left;
        }
        
        // Try to parse another term (implicit concatenation)
        Expression right = parseConcatenation(input);
        if (right == null) {
            if (hasExplicitOperator) {
                // Had explicit operator but no right operand - error
                error(input.rangeFrom(opPos), "Expected expression after concatenation operator '.'");
                return null;
            }
            // No explicit operator and can't parse more - just return left
            return left;
        }
        
        if (isSmart) {
            return left.concat(right);
        }
        return new Concatenation(left, right);
    }
    
    /**
     * Parse a Kleene star expression (highest precedence).
     * <p>
     * Grammar: F -> P '*'*
     * <p>
     * Right-associative: a** → (a*)*
     */
    private Expression parseKleene(Peekable input) {
        Expression expr = parsePrimary(input);
        if (expr == null) {
            return null;
        }
        
        // Check for star operator
        eatSpace(input);
        if (!input.hasNext() || input.peek() != '*') {
            return expr;
        }
        
        input.next(); // consume *
        
        // Right-recursive: apply star then check for more stars
        Expression starred;
        if (isSmart) {
            starred = expr.star();
        } else {
            starred = new KleeneStar(expr);
        }
        
        // Recursively handle multiple stars: a** → (a*)*
        eatSpace(input);
        if (input.hasNext() && input.peek() == '*') {
            // Parse the remaining stars recursively
            return parseKleeneSuffix(starred, input);
        }
        
        return starred;
    }
    
    /**
     * Helper to parse suffix stars right-associatively.
     */
    private Expression parseKleeneSuffix(Expression expr, Peekable input) {
        if (!input.hasNext() || input.peek() != '*') {
            return expr;
        }
        
        input.next(); // consume *
        
        Expression starred;
        if (isSmart) {
            starred = expr.star();
        } else {
            starred = new KleeneStar(expr);
        }
        
        // Recursively handle more stars
        eatSpace(input);
        return parseKleeneSuffix(starred, input);
    }
    
    /**
     * Parse a primary expression (atoms and parenthesized expressions).
     * <p>
     * Grammar: P -> ∅ | ϵ | τ[...] | (E)
     */
    private Expression parsePrimary(Peekable input) {
        eatSpace(input);
        
        if (!input.hasNext()) {
            return null;
        }
        
        char ch = input.peek();
        
        // Empty set
        if (ch == '∅') {
            input.next();
            return Expression.EMPTY;
        }
        
        // Epsilon
        if (ch == 'ϵ') {
            input.next();
            return Expression.EPSILON;
        }
        
        // Token
        if (ch == 'τ' || ch == 't') {
            return parseToken(input);
        }
        
        // Parenthesized expression
        if (ch == '(') {
            return parseParens(input);
        }
        
        // Unexpected character
        error(Range.at(input.position()), "Unexpected character '" + ch + "'");
        return null;
    }
    
    /**
     * Parse a token: τ[value] or t[value].
     * 
     * <p>If the token value is empty (τ[]), returns {@link Expression#EPSILON}
     * instead of creating an invalid empty token.
     */
    private Expression parseToken(Peekable input) {
        Position start = input.position();
        input.next(); // consume τ or t
        eatSpace(input);
        
        if (!input.hasNext()) {
            error(input.rangeFrom(start), "Expected '[' after token prefix");
            return null;
        }
        
        if (input.peek() != '[') {
            error(Range.at(input.position()), "Expected '[' after token prefix, got '" + input.peek() + "'");
            return null;
        }
        input.next(); // consume [
        
        Position valueStart = input.position();
        String value = readTokenValue(input);
        Position valueEnd = input.position();
        
        if (!input.hasNext()) {
            error(input.rangeFrom(start), "Unclosed token: missing ']'");
            return null;
        }
        
        if (input.peek() != ']') {
            error(Range.at(input.position()), "Expected ']' to close token, got '" + input.peek() + "'");
            return null;
        }
        input.next(); // consume ]
        
        // Empty token value should be represented as epsilon, not Token("")
        if (value.isEmpty()) {
            return Expression.EPSILON;
        }
        
        // Validate alien syntax in token value
        Range valueRange = new Range(valueStart, valueEnd);
        ParseResult<String> validationResult = alienValidator.validate(value, valueRange);
        
        if (validationResult instanceof ParseResult.Failure<String> failure) {
            // Add validation errors to our error list
            errors.addAll(failure.errors());
            return null;
        }
        
        return new Token(value);
    }
    
    /**
     * Parse a parenthesized expression: (E).
     */
    private Expression parseParens(Peekable input) {
        Position start = input.position();
        input.next(); // consume (
        
        Expression expr = parseUnion(input);
        if (expr == null) {
            error(input.rangeFrom(start), "Expected expression after '('");
            return null;
        }
        
        eatSpace(input);
        if (!input.hasNext()) {
            error(input.rangeFrom(start), "Unclosed parenthesis: missing ')'");
            return null;
        }
        
        if (input.peek() != ')') {
            error(Range.at(input.position()), "Expected ')' to close parenthesis, got '" + input.peek() + "'");
            return null;
        }
        input.next(); // consume )
        
        return expr;
    }
    
    /**
     * Skip whitespace characters.
     */
    private void eatSpace(Peekable input) {
        while (input.hasNext() && Character.isWhitespace(input.peek())) {
            input.next();
        }
    }
    
    /**
     * Read and process token value with escape sequence handling.
     * <p>
     * Escape sequences are processed:
     * <ul>
     *   <li>\n → newline</li>
     *   <li>\t → tab</li>
     *   <li>\r → carriage return</li>
     *   <li>\\ → backslash</li>
     *   <li>\] → closing bracket</li>
     *   <li>\[ → opening bracket</li>
     *   <li>\x (other) → \x (kept as-is)</li>
     * </ul>
     * <p>
     * The token value contains the interpreted string, not the escape syntax.
     * External tools receive the actual characters (e.g., a real newline, not "\n").
     */
    private String readTokenValue(Peekable input) {
        StringBuilder sb = new StringBuilder();
        
        while (input.hasNext()) {
            char ch = input.peek();
            if (ch == ']') {
                break;
            }
            
            input.next();
            
            // Handle escape sequences
            if (ch == '\\' && input.hasNext()) {
                char next = input.next();
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case ']' -> sb.append(']');
                    case '[' -> sb.append('[');
                    default -> {
                        sb.append('\\');
                        sb.append(next);
                    }
                }
            } else {
                sb.append(ch);
            }
        }
        
        return sb.toString();
    }
}
