package rege.syntax;

import rege.syntax.model.*;

/**
 * Parser for regular expressions that produces LEFT-ASSOCIATIVE parse trees.
 * 
 * Contrast with RegeReader which produces right-associative trees:
 * - RegeReader: a.b.c → a⋅(b⋅c)
 * - RegeReaderLeft: a.b.c → (a⋅b)⋅c
 * 
 * This parser uses an iterative approach to build left-associative structures,
 * which can be more natural for left-to-right processing (e.g., Brzozowski derivatives).
 * 
 * Grammar (same as RegeReader but with left-associative operators):
 * <pre>
 * E  -> T E'
 * E' -> | T E' | ∪ T E' | ε
 * T  -> F T'
 * T' -> . F T' | ⋅ F T' | F T' | ε
 * F  -> P F'
 * F' -> * F' | ε
 * P  -> ∅ | ϵ | τ[...] | (E)
 * </pre>
 */
public class RegeReaderLeft {
    
    private final boolean isSmart;
    
    private RegeReaderLeft(boolean isSmart) {
        this.isSmart = isSmart;
    }
    
    /**
     * Read an expression from a string, producing a left-associative parse tree.
     * Uses smart constructors by default.
     * 
     * @param input the string to parse
     * @return the parsed expression, or null if parsing fails
     */
    public static Expression readExpression(String input) {
        return readExpression(input, true);
    }
    
    /**
     * Read an expression from a string, producing a left-associative parse tree.
     * 
     * @param input the string to parse
     * @param isSmart whether to use smart constructors that apply simplification rules
     * @return the parsed expression, or null if parsing fails
     */
    public static Expression readExpression(String input, boolean isSmart) {
        return new RegeReaderLeft(isSmart).parseExpression(new Peekable(input));
    }
    
    /**
     * Parse a full expression (union has lowest precedence).
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
            
            input.next(); // consume operator
            Expression right = parseConcatenation(input);
            if (right == null) {
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
            if (hasExplicitOperator) {
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
        
        Expression expr = parseExpression(input);
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
     * Read characters until we hit ']', handling escape sequences.
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
