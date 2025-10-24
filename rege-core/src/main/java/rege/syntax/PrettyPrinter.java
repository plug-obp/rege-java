package rege.syntax;

import rege.syntax.model.*;

/**
 * A visitor that converts regular expressions to their textual representation.
 * 
 * <p>This pretty printer produces output compatible with {@link RegeReader}, using
 * Unicode mathematical symbols:
 * <ul>
 *   <li>∅ (U+2205) for empty language</li>
 *   <li>ϵ (U+03F5) for epsilon (empty string)</li>
 *   <li>τ[value] for tokens</li>
 *   <li>| for union</li>
 *   <li>⋅ for concatenation (optional, can be implicit)</li>
 *   <li>* for Kleene star</li>
 *   <li>( ) for grouping</li>
 * </ul>
 * 
 * <p>The printer handles operator precedence correctly:
 * <ol>
 *   <li>Star (highest precedence)</li>
 *   <li>Concatenation</li>
 *   <li>Union (lowest precedence)</li>
 * </ol>
 * 
 * <p><b>Escape Sequences:</b>
 * Token values contain interpreted characters. The printer escapes special characters:
 * <ul>
 *   <li>Newline → \n</li>
 *   <li>Tab → \t</li>
 *   <li>Backslash → \\</li>
 *   <li>Bracket ] → \]</li>
 * </ul>
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Expression expr = new Union(new Token("a"), new Token("b"));
 * String text = PrettyPrinter.print(expr);
 * // text = "τ[a]|τ[b]"
 * 
 * // Token with special characters
 * Expression expr2 = new Token("hello\nworld");
 * String text2 = PrettyPrinter.print(expr2);
 * // text2 = "τ[hello\nworld]"
 * }</pre>
 * 
 * <p><b>Roundtripping:</b>
 * The output is designed to be parseable by {@link RegeReader}:
 * <pre>{@code
 * Expression original = new Token("a]b");
 * String printed = PrettyPrinter.print(original);  // "τ[a\]b]"
 * Expression parsed = RegeReader.readExpression(printed);
 * assert original.equals(parsed);  // true
 * }</pre>
 * 
 * @see RegeReader
 * @see RegeReaderLeft
 */
public class PrettyPrinter implements Visitor<Integer, String> {
    
    // Precedence levels (higher = binds tighter)
    private static final int PRECEDENCE_UNION = 1;
    private static final int PRECEDENCE_CONCAT = 2;
    private static final int PRECEDENCE_STAR = 3;
    private static final int PRECEDENCE_ATOM = 4;
    
    private final boolean explicitConcatenation;
    
    /**
     * Create a pretty printer.
     * 
     * @param explicitConcatenation if true, print concatenation operator (⋅)
     */
    public PrettyPrinter(boolean explicitConcatenation) {
        this.explicitConcatenation = explicitConcatenation;
    }
    
    /**
     * Create a pretty printer with implicit concatenation (no ⋅ operator).
     */
    public PrettyPrinter() {
        this(false);
    }
    
    /**
     * Print an expression to its textual representation.
     * 
     * @param expression the expression to print
     * @return the textual representation
     */
    public static String print(Expression expression) {
        return expression.accept(new PrettyPrinter(), PRECEDENCE_UNION);
    }
    
    /**
     * Print an expression to its textual representation.
     * 
     * @param expression the expression to print
     * @param explicitConcatenation if true, print concatenation operator (⋅)
     * @return the textual representation
     */
    public static String print(Expression expression, boolean explicitConcatenation) {
        return expression.accept(new PrettyPrinter(explicitConcatenation), PRECEDENCE_UNION);
    }
    
    @Override
    public String visitToken(Token token, Integer parentPrecedence) {
        // Token values contain interpreted characters. We need to escape them for the syntax.
        // This ensures roundtrip: parse(print(expr)) == expr
        StringBuilder escaped = new StringBuilder();
        for (char c : token.value().toCharArray()) {
            switch (c) {
                case '\n' -> escaped.append("\\n");
                case '\t' -> escaped.append("\\t");
                case '\r' -> escaped.append("\\r");
                case '\\' -> escaped.append("\\\\");
                case ']' -> escaped.append("\\]");
                case '[' -> escaped.append("\\[");
                default -> escaped.append(c);
            }
        }
        return "τ[" + escaped + "]";
    }
    
    @Override
    public String visitEmpty(Empty empty, Integer parentPrecedence) {
        return "∅";
    }
    
    @Override
    public String visitEpsilon(Epsilon epsilon, Integer parentPrecedence) {
        return "ϵ";
    }
    
    @Override
    public String visitUnion(Union union, Integer parentPrecedence) {
        // Concatenation children need parens ONLY when using implicit concatenation
        // With explicit "⋅", precedence is clear: "a⋅b|c⋅d" is unambiguous
        // Without "⋅", implicit concatenation needs parens: "(ab)|c"
        boolean needsParens = !explicitConcatenation;
        
        String left = (needsParens && union.lhs() instanceof Concatenation)
            ? "(" + union.lhs().accept(this, PRECEDENCE_UNION) + ")"
            : union.lhs().accept(this, PRECEDENCE_UNION);
        String right = (needsParens && union.rhs() instanceof Concatenation)
            ? "(" + union.rhs().accept(this, PRECEDENCE_UNION) + ")"
            : union.rhs().accept(this, PRECEDENCE_UNION);
        String result = left + "|" + right;
        
        // Add parentheses if parent has higher precedence
        if (parentPrecedence > PRECEDENCE_UNION) {
            result = "(" + result + ")";
        }
        
        return result;
    }
    
    @Override
    public String visitConcatenation(Concatenation concatenation, Integer parentPrecedence) {
        // Visit children - precedence mechanism handles when they need parens
        String left = concatenation.lhs().accept(this, PRECEDENCE_CONCAT);
        String right = concatenation.rhs().accept(this, PRECEDENCE_CONCAT);
        
        String result;
        if (explicitConcatenation) {
            result = left + "⋅" + right;
        } else {
            result = left + right;
        }
        
        // Add parentheses if parent has higher precedence
        if (parentPrecedence > PRECEDENCE_CONCAT) {
            result = "(" + result + ")";
        }
        
        return result;
    }
    
    @Override
    public String visitKleeneStar(KleeneStar kleeneStar, Integer parentPrecedence) {
        String inner = kleeneStar.expression().accept(this, PRECEDENCE_STAR);
        
        // Atoms don't need parentheses under star
        if (kleeneStar.expression() instanceof Token 
            || kleeneStar.expression() instanceof Empty
            || kleeneStar.expression() instanceof Epsilon) {
            return inner + "*";
        }
        
        // Composite expressions need parentheses
        return "(" + kleeneStar.expression().accept(this, PRECEDENCE_UNION) + ")*";
    }
    
    @Override
    public String visitExpression(Expression expression, Integer parentPrecedence) {
        return expression.toString();
    }
}
