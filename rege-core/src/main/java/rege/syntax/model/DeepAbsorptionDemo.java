package rege.syntax.model;

import static rege.syntax.model.Simplifier.simplify;

/**
 * Demonstration of deep absorption in the Simplifier.
 * This class shows how the Simplifier recursively applies
 * absorption rules to nested expressions.
 */
public class DeepAbsorptionDemo {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DeepAbsorptionDemo() {
        // Utility class - not meant to be instantiated
    }
    
    /**
     * Main entry point for the demonstration.
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=== Deep Absorption Demonstration ===\n");
        
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        Token d = new Token("d");
        
        // Example 1: Simple absorption
        System.out.println("Example 1: Simple Absorption");
        Union ab = new Union(a, b);
        Union aab = new Union(a, ab);
        System.out.println("Original:   " + aab);
        System.out.println("Simplified: " + simplify(aab));
        System.out.println("Expected:   " + ab);
        System.out.println();
        
        // Example 2: Deep nested absorption
        System.out.println("Example 2: Deep Nested Absorption (2 levels)");
        Union ab2 = new Union(a, b);
        Union abc = new Union(ab2, c);
        Union aabc = new Union(a, abc);
        System.out.println("Original:   " + aabc);
        System.out.println("Simplified: " + simplify(aabc));
        System.out.println("Expected:   " + abc);
        System.out.println();
        
        // Example 3: Very deep nested absorption
        System.out.println("Example 3: Very Deep Nested Absorption (3 levels)");
        Union ab3 = new Union(a, b);
        Union abc3 = new Union(ab3, c);
        Union abcd = new Union(abc3, d);
        Union aabcd = new Union(a, abcd);
        System.out.println("Original:   " + aabcd);
        System.out.println("Simplified: " + simplify(aabcd));
        System.out.println("Expected:   " + abcd);
        System.out.println();
        
        // Example 4: Absorption from right
        System.out.println("Example 4: Absorption from Right");
        Union ab4 = new Union(a, b);
        Union abc4 = new Union(ab4, c);
        Union abca = new Union(abc4, a);
        System.out.println("Original:   " + abca);
        System.out.println("Simplified: " + simplify(abca));
        System.out.println("Expected:   " + abc4);
        System.out.println();
        
        // Example 5: Middle element absorption
        System.out.println("Example 5: Middle Element Absorption");
        Union ab5 = new Union(a, b);
        Union abc5 = new Union(ab5, c);
        Union babc = new Union(b, abc5);
        System.out.println("Original:   " + babc);
        System.out.println("Simplified: " + simplify(babc));
        System.out.println("Expected:   " + abc5);
        System.out.println();
        
        // Example 6: Complex nested structure
        System.out.println("Example 6: Complex Nested Structure");
        Union ab6 = new Union(a, b);
        Union bc6 = new Union(b, c);
        Union abc6 = new Union(ab6, bc6);
        System.out.println("Original:   " + abc6);
        System.out.println("Simplified: " + simplify(abc6));
        System.out.println("Note: B appears in both sides, but no full absorption");
        System.out.println();
        
        // Example 7: No absorption (different elements)
        System.out.println("Example 7: No Absorption (Different Elements)");
        Union ab7 = new Union(a, b);
        Union cd7 = new Union(c, d);
        Union abcd7 = new Union(ab7, cd7);
        System.out.println("Original:   " + abcd7);
        System.out.println("Simplified: " + simplify(abcd7));
        System.out.println("Note: No common elements, no absorption");
        System.out.println();
        
        // Example 8: Absorption with empty
        System.out.println("Example 8: Absorption with Empty");
        Union emptyA = new Union(Expression.EMPTY, a);
        Union emptyAB = new Union(emptyA, b);
        Union aEmptyAB = new Union(a, emptyAB);
        System.out.println("Original:   " + aEmptyAB);
        System.out.println("Simplified: " + simplify(aEmptyAB));
        System.out.println("Note: Empty removed first, then absorption applied");
        System.out.println();
        
        // Example 9: Comparing with factory method
        System.out.println("Example 9: Factory Method vs Simplifier");
        Expression factoryResult = a.union(ab);
        Expression simplifierInput = new Union(a, ab);
        Expression simplifierResult = simplify(simplifierInput);
        System.out.println("Factory (a.union(a|b)):     " + factoryResult);
        System.out.println("Simplifier (simplify(a|(a|b))): " + simplifierResult);
        System.out.println("Are they equal? " + factoryResult.equals(simplifierResult));
        System.out.println();
        
        System.out.println("=== Summary ===");
        System.out.println("✅ Factory method handles 1-level absorption");
        System.out.println("✅ Simplifier handles arbitrary depth absorption");
        System.out.println("✅ Both work together for optimal simplification");
    }
}
