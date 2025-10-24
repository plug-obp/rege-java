package rege.syntax;

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
 * <p>Usage:
 * <pre>{@code
 * Expression expr = RegeReader.readExpression("τ[a] | τ[b]");
 * Expression smart = RegeReader.readExpression("ϵ | ϵ", true); // Uses smart constructors
 * }</pre>
 */
public class RegeReader {
    
    private final boolean isSmart;
    
    /**
     * Create a reader with specified smartness.
     * 
     * @param isSmart if true, uses smart constructors (union(), concat(), star())
     *                which apply simplification rules
     */
    private RegeReader(boolean isSmart) {
        this.isSmart = isSmart;
    }
    
    /**
     * Read an expression from string with smart constructors.
     * 
     * @param input the input string
     * @return the parsed expression, or null if parsing fails
     */
    public static Expression readExpression(String input) {
        return readExpression(input, true);
    }
    
    /**
     * Read an expression from string.
     * 
     * @param input the input string
     * @param isSmart if true, uses smart constructors
     * @return the parsed expression, or null if parsing fails
     */
    public static Expression readExpression(String input, boolean isSmart) {
        return new RegeReader(isSmart).parseUnion(new Peekable(input));
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
        
        input.next(); // consume operator
        
        // Right-recursive: parse rest as another union
        Expression right = parseUnion(input);
        if (right == null) {
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
        if (hasExplicitOperator) {
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
        
        return null;
    }
    
    /**
     * Parse a token: τ[value] or t[value].
     */
    private Expression parseToken(Peekable input) {
        input.next(); // consume τ or t
        eatSpace(input);
        
        if (!input.hasNext() || input.peek() != '[') {
            return null;
        }
        input.next(); // consume [
        
        String value = readTokenValue(input);
        
        if (!input.hasNext() || input.peek() != ']') {
            return null;
        }
        input.next(); // consume ]
        
        return new Token(value);
    }
    
    /**
     * Parse a parenthesized expression: (E).
     */
    private Expression parseParens(Peekable input) {
        input.next(); // consume (
        
        Expression expr = parseUnion(input);
        if (expr == null) {
            return null;
        }
        
        eatSpace(input);
        if (!input.hasNext() || input.peek() != ')') {
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
