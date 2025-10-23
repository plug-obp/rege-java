package rege.syntax;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that demonstrate the structural differences between RegeReader and RegeReaderLeft
 * while verifying they are semantically equivalent.
 */
class RegeReaderComparisonTest {
    
    @Test
    void demonstrateConcatenationDifference() {
        String input = "τ[a].τ[b].τ[c]";
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        // They should NOT be equal (different structures)
        assertNotEquals(right, left, "Different associativity should produce different structures");
        
        // RegeReader: a⋅(b⋅c) - right child is Concatenation
        assertTrue(right instanceof Concatenation);
        Concatenation rightOuter = (Concatenation) right;
        assertEquals(new Token("a"), rightOuter.lhs());
        assertTrue(rightOuter.rhs() instanceof Concatenation, "Right-associative: right child should be Concatenation");
        
        // RegeReaderLeft: (a⋅b)⋅c - left child is Concatenation
        assertTrue(left instanceof Concatenation);
        Concatenation leftOuter = (Concatenation) left;
        assertEquals(new Token("c"), leftOuter.rhs());
        assertTrue(leftOuter.lhs() instanceof Concatenation, "Left-associative: left child should be Concatenation");
        
        System.out.println("✓ Concatenation: Different structures verified");
        System.out.println("  Right-assoc (RegeReader):     a⋅(b⋅c)");
        System.out.println("  Left-assoc (RegeReaderLeft): (a⋅b)⋅c");
    }
    
    @Test
    void demonstrateUnionDifference() {
        String input = "τ[a]|τ[b]|τ[c]";
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        // They should NOT be equal (different structures)
        assertNotEquals(right, left);
        
        // RegeReader: a|(b|c) - right child is Union
        assertTrue(right instanceof Union);
        Union rightOuter = (Union) right;
        assertEquals(new Token("a"), rightOuter.lhs());
        assertTrue(rightOuter.rhs() instanceof Union, "Right-associative: right child should be Union");
        
        // RegeReaderLeft: (a|b)|c - left child is Union
        assertTrue(left instanceof Union);
        Union leftOuter = (Union) left;
        assertEquals(new Token("c"), leftOuter.rhs());
        assertTrue(leftOuter.lhs() instanceof Union, "Left-associative: left child should be Union");
        
        System.out.println("✓ Union: Different structures verified");
        System.out.println("  Right-assoc (RegeReader):     a|(b|c)");
        System.out.println("  Left-assoc (RegeReaderLeft): (a|b)|c");
    }
    
    @Test
    void demonstrateLongChainDifference() {
        String input = "τ[a].τ[b].τ[c].τ[d]";
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        // Count nesting depth
        int rightDepth = getLeftDepth(right);
        int leftDepth = getLeftDepth(left);
        
        System.out.println("✓ Long chain comparison:");
        System.out.println("  Input: a.b.c.d");
        System.out.println("  Right-assoc depth (left spine): " + rightDepth + " [a⋅(b⋅(c⋅d))]");
        System.out.println("  Left-assoc depth (left spine):  " + leftDepth + " [((a⋅b)⋅c)⋅d]");
        
        assertEquals(1, rightDepth, "Right-associative should have shallow left spine");
        assertEquals(3, leftDepth, "Left-associative should have deep left spine");
    }
    
    @Test
    void verifySemanticEquivalenceWithSmartConstructors() {
        // These should produce IDENTICAL results with smart constructors
        String[] equivalentInputs = {
            "∅*",
            "ϵ*", 
            "∅|τ[a]",
            "τ[a]|∅",
            "ϵ.τ[a]",
            "τ[a].ϵ",
            "∅.τ[a]",
            "τ[a].∅"
        };
        
        System.out.println("✓ Semantic equivalence with smart constructors:");
        for (String input : equivalentInputs) {
            Expression right = RegeReader.readExpression(input, true);
            Expression left = RegeReaderLeft.readExpression(input, true);
            assertEquals(right, left, "Smart constructors should produce identical results for: " + input);
            System.out.println("  " + input + " → " + formatExpression(right));
        }
    }
    
    @Test
    void verifyPrecedenceIsIdentical() {
        // Precedence should be the same in both parsers
        String input = "τ[a]|τ[b].τ[c]*";
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        // Both should parse as: a | (b . (c*))
        // i.e., Union at root, Concatenation on one side, Star deepest
        
        assertTrue(right instanceof Union);
        assertTrue(left instanceof Union);
        
        Union rightUnion = (Union) right;
        Union leftUnion = (Union) left;
        
        // Both should have Token("a") on lhs of union
        assertEquals(new Token("a"), rightUnion.lhs());
        assertEquals(new Token("a"), leftUnion.lhs());
        
        // Both should have Concatenation on rhs of union
        assertTrue(rightUnion.rhs() instanceof Concatenation);
        assertTrue(leftUnion.rhs() instanceof Concatenation);
        
        System.out.println("✓ Operator precedence identical in both parsers");
        System.out.println("  Input: a|b.c*");
        System.out.println("  Parsed as: a|(b⋅(c*)) in both cases");
    }
    
    /**
     * Count how deep the left spine goes (useful for measuring associativity).
     */
    private int getLeftDepth(Expression expr) {
        if (!(expr instanceof Concatenation concat)) {
            return 0;
        }
        return 1 + getLeftDepth(concat.lhs());
    }
    
    /**
     * Format an expression for display.
     */
    private String formatExpression(Expression expr) {
        if (expr instanceof Token token) {
            return "τ[" + token.value() + "]";
        } else if (expr == Expression.EMPTY) {
            return "∅";
        } else if (expr == Expression.EPSILON) {
            return "ε";
        } else if (expr instanceof KleeneStar star) {
            return "(" + formatExpression(star.expression()) + ")*";
        } else if (expr instanceof Concatenation concat) {
            return formatExpression(concat.lhs()) + "⋅" + formatExpression(concat.rhs());
        } else if (expr instanceof Union union) {
            return formatExpression(union.lhs()) + "|" + formatExpression(union.rhs());
        }
        return expr.toString();
    }
}
