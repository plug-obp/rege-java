package rege.syntax.model;

/**
 * Example demonstrating the use of the Simplifier.
 * This class shows various simplification rules applied to expressions.
 */
public class SimplifierExample {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SimplifierExample() {
        // Utility class - not meant to be instantiated
    }
    
    /**
     * Main entry point for the examples.
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=== Regular Expression Simplifier Examples ===\n");
        
        // Example 1: Nested Kleene stars
        System.out.println("Example 1: Nested Kleene stars");
        Token a = new Token("a");
        Expression nested = new KleeneStar(new KleeneStar(new KleeneStar(a)));
        System.out.println("Original:   " + nested);
        System.out.println("Simplified: " + Simplifier.simplify(nested));
        System.out.println();
        
        // Example 2: Union with empty
        System.out.println("Example 2: Union with empty");
        Token b = new Token("b");
        Expression unionEmpty = new Union(Expression.EMPTY, new Union(a, b));
        System.out.println("Original:   " + unionEmpty);
        System.out.println("Simplified: " + Simplifier.simplify(unionEmpty));
        System.out.println();
        
        // Example 3: Concatenation with epsilon and empty
        System.out.println("Example 3: Concatenation with epsilon");
        Expression concatEpsilon = new Concatenation(
            Expression.EPSILON, 
            new Concatenation(a, Expression.EPSILON)
        );
        System.out.println("Original:   " + concatEpsilon);
        System.out.println("Simplified: " + Simplifier.simplify(concatEpsilon));
        System.out.println();
        
        // Example 4: Complex nested expression
        System.out.println("Example 4: Complex nested expression");
        Expression complex = new KleeneStar(
            new Union(
                new Concatenation(Expression.EMPTY, a),
                new Concatenation(Expression.EPSILON, b)
            )
        );
        System.out.println("Original:   " + complex);
        System.out.println("Simplified: " + Simplifier.simplify(complex));
        System.out.println();
        
        // Example 5: Star of empty
        System.out.println("Example 5: Star of empty");
        Expression starEmpty = new KleeneStar(Expression.EMPTY);
        System.out.println("Original:   " + starEmpty);
        System.out.println("Simplified: " + Simplifier.simplify(starEmpty));
        System.out.println();
        
        // Example 6: Already simplified expression
        System.out.println("Example 6: Already simplified (no change)");
        Expression simple = new Concatenation(a, b);
        Expression simplified = Simplifier.simplify(simple);
        System.out.println("Original:   " + simple);
        System.out.println("Simplified: " + simplified);
        System.out.println("Same object: " + (simple == simplified));
    }
}
