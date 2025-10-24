package rege.syntax;

import rege.syntax.model.*;

/**
 * Parser for regular expressions that produces LEFT-ASSOCIATIVE parse trees.
 * 
 * <p>This parser implements standard regex precedence (highest to lowest):
 * <ol>
 *   <li>Kleene star (*) - highest precedence</li>
 *   <li>Concatenation (⋅) - middle precedence</li>
 *   <li>Union (|) - lowest precedence</li>
 * </ol>
 * 
 * <p>Operators are LEFT-ASSOCIATIVE:
 * <ul>
 *   <li>a.b.c → (a⋅b)⋅c</li>
 *   <li>a|b|c → (a|b)|c</li>
 *   <li>a** → (a*)* (star is special: right-associative for correct semantics)</li>
 * </ul>
 * 
 * <p>Contrast with RegeReader which produces right-associative trees:
 * <ul>
 *   <li>RegeReader: a.b.c → a⋅(b⋅c)</li>
 *   <li>RegeReaderLeft: a.b.c → (a⋅b)⋅c</li>
 * </ul>
 * 
 * <p>This parser uses an iterative approach to build left-associative structures,
 * which can be more natural for left-to-right processing (e.g., Brzozowski derivatives).
 * 
 * <p>Grammar (iterative with proper precedence):
 * <pre>
 * E  -> T ('|'|'∪' T)*              // Union (lowest precedence, left-assoc)
 * T  -> F ('.'|'⋅' F | F)*          // Concatenation (middle precedence, left-assoc)
 * F  -> P '*'*                       // Kleene star (highest precedence)
 * P  -> ∅ | ϵ | τ[...] | (E)        // Primary (atoms)
 * </pre>
 * 
 * <p>Returns {@link ParseResult} with either a successfully parsed expression
 * or a list of errors with precise position information.
 * 
 * <p>Usage:
 * <pre>{@code
 * ParseResult result = RegeReaderLeft.parse("τ[a] | τ[b]");
 * switch (result) {
 *     case ParseResult.Success(var expr) -> System.out.println("Parsed: " + expr);
 *     case ParseResult.Failure(var errors, var source) -> 
 *         errors.forEach(e -> System.err.println(e.formatWithSource(source)));
 * }
 * }</pre>
 */
public class RegeReaderLeft {
    
    private final boolean isSmart;
    private final java.util.List<ParseError> errors = new java.util.ArrayList<>();
    
    private RegeReaderLeft(boolean isSmart) {
        this.isSmart = isSmart;
    }
    
    /**
     * Parse an expression from string, producing a left-associative parse tree.
     * Uses smart constructors by default.
     * 
     * @param input the string to parse
     * @return parse result containing either the expression or errors
     */
    public static ParseResult parse(String input) {
        return parse(input, true);
    }
    
    /**
     * Parse an expression from string, producing a left-associative parse tree.
     * 
     * @param input the string to parse
     * @param isSmart whether to use smart constructors that apply simplification rules
     * @return parse result containing either the expression or errors
     */
    public static ParseResult parse(String input, boolean isSmart) {
        RegeReaderLeft reader = new RegeReaderLeft(isSmart);
        Peekable peekable = new Peekable(input);
        Expression expr = reader.parseExpression(peekable);
        
        if (!reader.errors.isEmpty()) {
            return new ParseResult.Failure(reader.errors, input);
        }
        
        if (expr == null) {
            // No errors but failed to parse - generic error
            reader.error(peekable.rangeHere(), "Failed to parse expression");
            return new ParseResult.Failure(reader.errors, input);
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
            return new ParseResult.Failure(reader.errors, input);
        }
        
        return new ParseResult.Success(expr);
    }
    
    /**
     * Legacy method for backward compatibility.
     * @deprecated Use {@link #parse(String)} instead
     */
    @Deprecated
    public static Expression readExpression(String input) {
        return parse(input, true).orElse(null);
    }
    
    /**
     * Legacy method for backward compatibility.
     * @deprecated Use {@link #parse(String, boolean)} instead
     */
    @Deprecated
    public static Expression readExpression(String input, boolean isSmart) {
        return parse(input, isSmart).orElse(null);
    }
    
    /**
     * Record an error at the specified range.
     */
    private void error(Range range, String message) {
        errors.add(new ParseError(range, message));
    }
    
    /**
     * Parse a full expression (union has lowest precedence).
     * <p>
     * Left-associative: a|b|c → (a|b)|c
     */
    private Expression parseExpression(Peekable input) {
        Expression left = parseConcatenation(input);
        if (left == null) {
            return null;
        }
        
        // Iteratively consume union operators (left-associative)
        while (true) {
            eatSpace(input);
            if (!input.hasNext()) {
                break;
            }
            
            char ch = input.peek();
            if (ch != '|' && ch != '∪') {
                break;
            }
            
            Position opPos = input.position();
            input.next(); // consume operator
            Expression right = parseConcatenation(input);
            if (right == null) {
                error(input.rangeFrom(opPos), "Expected expression after union operator '|'");
                return null;
            }
            
            // Build left-associative: (left | right)
            if (isSmart) {
                left = left.union(right);
            } else {
                left = new Union(left, right);
            }
        }
        
        return left;
    }
    
    /**
     * Parse concatenation (higher precedence than union).
     * <p>
     * Left-associative: a.b.c → (a⋅b)⋅c
     */
    private Expression parseConcatenation(Peekable input) {
        Expression left = parseKleene(input);
        if (left == null) {
            return null;
        }
        
        // Iteratively consume concatenation operators or adjacent terms (left-associative)
        while (true) {
            eatSpace(input);
            if (!input.hasNext()) {
                break;
            }
            
            char ch = input.peek();
            
            // Check for explicit concatenation operators
            boolean hasExplicitOperator = (ch == '.' || ch == '⋅');
            Position opPos = null;
            if (hasExplicitOperator) {
                opPos = input.position();
                input.next(); // consume operator
            }
            
            // Stop if we see a union or closing paren (lower precedence or end of group)
            if (ch == '|' || ch == '∪' || ch == ')') {
                break;
            }
            
            // Try to parse another term
            Expression right = parseKleene(input);
            if (right == null) {
                // If we had an explicit operator, this is an error
                if (hasExplicitOperator) {
                    error(input.rangeFrom(opPos), "Expected expression after concatenation operator '.'");
                    return null;
                }
                // Otherwise, just stop (no more terms to concatenate)
                break;
            }
            
            // Build left-associative: (left . right)
            if (isSmart) {
                left = left.concat(right);
            } else {
                left = new Concatenation(left, right);
            }
        }
        
        return left;
    }
    
    /**
     * Parse Kleene star (highest precedence).
     * <p>
     * Note: Star itself is right-associative for correct semantics: a** → (a*)*
     * This is consistent with standard regex behavior.
     */
    private Expression parseKleene(Peekable input) {
        Expression expr = parsePrimary(input);
        if (expr == null) {
            return null;
        }
        
        // Iteratively apply star operators (right-associative: a** = (a*)*)
        while (true) {
            eatSpace(input);
            if (!input.hasNext() || input.peek() != '*') {
                break;
            }
            
            input.next(); // consume *
            
            if (isSmart) {
                expr = expr.star();
            } else {
                expr = new KleeneStar(expr);
            }
        }
        
        return expr;
    }
    
    /**
     * Parse primary expressions (atoms).
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
        
        String value = readTokenValue(input);
        
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
        
        return new Token(value);
    }
    
    /**
     * Parse a parenthesized expression: (E).
     */
    private Expression parseParens(Peekable input) {
        Position start = input.position();
        input.next(); // consume (
        
        Expression expr = parseExpression(input);
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
    
    /**
     * Skip whitespace characters.
     */
    private void eatSpace(Peekable input) {
        while (input.hasNext() && Character.isWhitespace(input.peek())) {
            input.next();
        }
    }
}
