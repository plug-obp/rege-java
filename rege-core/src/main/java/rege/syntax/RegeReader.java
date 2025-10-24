package rege.syntax;

import rege.syntax.model.*;

/**
 * Reader for parsing regular expressions from text format.
 * <p>
 * Grammar:
 * <pre>
 * E  -> ∅ E'              // empty
 *     | ϵ E'              // epsilon
 *     | τ[string] E'      // token
 *     | (E) E'            // parentheses
 * 
 * E' -> | E E'            // union
 *     | . E E'            // concatenation
 *     | ⋅ E E'            // concatenation (alternative symbol)
 *     | * E'              // kleene star
 *     | ϵ                 // empty (end of input)
 * </pre>
 * <p>
 * Usage:
 * <pre>{@code
 * Expression expr = RegeReader.readExpression("τ[a] | τ[b]");
 * Expression smart = RegeReader.readExpression("ϵ | ϵ", true); // Uses smart constructors
 * }</pre>
 */
public class RegeReader {
    
    private Expression context;
    private final boolean isSmart;
    
    /**
     * Create a reader with specified smartness.
     * 
     * @param isSmart if true, uses smart constructors (union(), concat(), star())
     *                which apply simplification rules
     */
    public RegeReader(boolean isSmart) {
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
        return new RegeReader(isSmart).readExpression(new Peekable(input));
    }
    
    /**
     * Read an expression from the peekable input.
     * 
     * @param input the peekable input
     * @return the parsed expression, or null if parsing fails
     */
    public Expression readExpression(Peekable input) {
        eatSpace(input);
        
        Expression expr = readEmpty(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        expr = readEpsilon(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        expr = readToken(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        expr = readParens(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        return null;
    }
    
    private Expression getRemaining(Peekable input, Expression expr) {
        this.context = expr;
        Expression result = readExpressionPrim(input);
        if (result == null) {
            result = this.context;
        }
        this.context = null;
        return result;
    }
    
    private Expression readExpressionPrim(Peekable input) {
        eatSpace(input);
        
        Expression expr = readConcatenation(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        expr = readUnion(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        expr = readKleeneStar(input);
        if (expr != null) {
            return getRemaining(input, expr);
        }
        
        if (input.hasNext()) {
            return null;
        }
        
        return this.context;
    }
    
    private Expression readEmpty(Peekable input) {
        if (!input.hasNext() || input.peek() != '∅') {
            return null;
        }
        input.next();
        return Expression.EMPTY;
    }
    
    private Expression readEpsilon(Peekable input) {
        if (!input.hasNext() || input.peek() != 'ϵ') {
            return null;
        }
        input.next();
        return Expression.EPSILON;
    }
    
    private Expression readToken(Peekable input) {
        if (!input.hasNext() || (input.peek() != 'τ' && input.peek() != 't')) {
            return null;
        }
        input.next();
        eatSpace(input);
        
        if (!input.hasNext() || input.peek() != '[') {
            return null;
        }
        input.next();
        
        String value = readTokenValue(input);
        
        if (!input.hasNext() || input.peek() != ']') {
            return null;
        }
        input.next();
        
        return new Token(value);
    }
    
    private Expression readConcatenation(Peekable input) {
        Expression lhs = this.context;
        if (!input.hasNext()) {
            return null;
        }
        
        // '.' and '⋅' are optional concatenation operators
        if (input.peek() == '.' || input.peek() == '⋅') {
            input.next();
        }
        
        Expression rhs = readExpression(input);
        if (rhs == null) {
            return null;
        }
        
        if (isSmart) {
            return lhs.concat(rhs);
        }
        return new Concatenation(lhs, rhs);
    }
    
    private Expression readUnion(Peekable input) {
        Expression lhs = this.context;
        if (!input.hasNext() || (input.peek() != '|' && input.peek() != '∪')) {
            return null;
        }
        input.next();
        
        Expression rhs = readExpression(input);
        if (rhs == null) {
            return null;
        }
        
        if (isSmart) {
            return lhs.union(rhs);
        }
        return new Union(lhs, rhs);
    }
    
    private Expression readKleeneStar(Peekable input) {
        if (!input.hasNext() || input.peek() != '*') {
            return null;
        }
        input.next();
        
        if (isSmart) {
            return this.context.star();
        }
        return new KleeneStar(this.context);
    }
    
    private Expression readParens(Peekable input) {
        if (!input.hasNext() || input.peek() != '(') {
            return null;
        }
        input.next();
        
        Expression expr = readExpression(input);
        
        if (!input.hasNext() || input.peek() != ')') {
            return null;
        }
        input.next();
        
        return expr;
    }
    
    private void eatSpace(Peekable input) {
        while (input.hasNext() && (input.peek() == ' ' || input.peek() == '\t')) {
            input.next();
        }
    }
    
    private String readTokenValue(Peekable input) {
        StringBuilder token = new StringBuilder();
        char precedent = '\0';
        
        while (input.hasNext()) {
            char current = input.peek();
            
            // Stop at ']' unless it's escaped with '\'
            if (current == ']' && precedent != '\\') {
                break;
            }
            
            precedent = current;
            token.append(current);
            input.next();
        }
        
        return token.toString();
    }
}
