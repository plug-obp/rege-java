package rege.syntax.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for deep absorption in the Simplifier.
 */
class SimplifierAbsorptionTest {
    
    @Test
    void testSimpleAbsorption() {
        // A | (A | B) = A | B
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union ab = new Union(a, b);
        Union aab = new Union(a, ab);
        
        Expression result = Simplifier.simplify(aab);
        assertEquals(ab, result, "A | (A | B) should simplify to A | B");
    }
    
    @Test
    void testAbsorptionCommutative() {
        // A | (B | A) = B | A
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union ba = new Union(b, a);
        Union aba = new Union(a, ba);
        
        Expression result = Simplifier.simplify(aba);
        assertEquals(ba, result, "A | (B | A) should simplify to B | A");
    }
    
    @Test
    void testDeepNestedAbsorption() {
        // A | ((A | B) | C) should simplify because A is nested inside
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union result = new Union(a, abc);
        
        Expression simplified = Simplifier.simplify(result);
        
        // After simplification, A should be absorbed since it's in the nested union
        assertEquals(abc, simplified, "A | ((A | B) | C) should simplify to (A | B) | C");
    }
    
    @Test
    void testVeryDeeplyNestedAbsorption() {
        // A | (((A | B) | C) | D) - three levels deep
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        Token d = new Token("d");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union abcd = new Union(abc, d);
        Union result = new Union(a, abcd);
        
        Expression simplified = Simplifier.simplify(result);
        
        assertEquals(abcd, simplified, "A | (((A | B) | C) | D) should simplify to ((A | B) | C) | D");
    }
    
    @Test
    void testAbsorptionFromRight() {
        // (A | B) | A = A | B
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union ab = new Union(a, b);
        Union aba = new Union(ab, a);
        
        Expression result = Simplifier.simplify(aba);
        assertEquals(ab, result, "(A | B) | A should simplify to A | B");
    }
    
    @Test
    void testDeepAbsorptionFromRight() {
        // ((A | B) | C) | A should simplify
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union result = new Union(abc, a);
        
        Expression simplified = Simplifier.simplify(result);
        assertEquals(abc, simplified, "((A | B) | C) | A should simplify to (A | B) | C");
    }
    
    @Test
    void testAbsorptionMiddleElement() {
        // B | ((A | B) | C) - B is in the middle of the nested union
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union result = new Union(b, abc);
        
        Expression simplified = Simplifier.simplify(result);
        assertEquals(abc, simplified, "B | ((A | B) | C) should simplify to (A | B) | C");
    }
    
    @Test
    void testAbsorptionRightElement() {
        // C | ((A | B) | C) - C is on the right of the nested union
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union result = new Union(c, abc);
        
        Expression simplified = Simplifier.simplify(result);
        assertEquals(abc, simplified, "C | ((A | B) | C) should simplify to (A | B) | C");
    }
    
    @Test
    void testNoAbsorptionWhenNotPresent() {
        // D | ((A | B) | C) - D is not in the nested union
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        Token d = new Token("d");
        
        Union ab = new Union(a, b);
        Union abc = new Union(ab, c);
        Union result = new Union(d, abc);
        
        Expression simplified = Simplifier.simplify(result);
        
        // Should create a new union with simplified children
        assertTrue(simplified instanceof Union, "Should still be a union");
        Union simplifiedUnion = (Union) simplified;
        assertEquals(d, simplifiedUnion.lhs());
        assertEquals(abc, simplifiedUnion.rhs());
    }
    
    @Test
    void testComplexNestedAbsorption() {
        // (A | B) | ((A | B) | C) - left side is completely contained in right
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab1 = new Union(a, b);
        Union ab2 = new Union(a, b);
        Union abc = new Union(ab2, c);
        Union result = new Union(ab1, abc);
        
        Expression simplified = Simplifier.simplify(result);
        
        // Since (A | B) equals (A | B) in the nested union, it should absorb
        assertEquals(abc, simplified, "(A | B) | ((A | B) | C) should simplify to (A | B) | C");
    }
    
    @Test
    void testMultiLevelAbsorptionLeftToRight() {
        // Create: A | (B | (A | C))
        // A should be absorbed from the deeply nested union
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ac = new Union(a, c);
        Union bac = new Union(b, ac);
        Union result = new Union(a, bac);
        
        Expression simplified = Simplifier.simplify(result);
        assertEquals(bac, simplified, "A | (B | (A | C)) should simplify to B | (A | C)");
    }
    
    @Test
    void testAbsorptionWithEmpty() {
        // A | ((∅ | A) | B) should simplify
        // First ∅ | A = A, then we have A | (A | B) = A | B
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union emptyA = new Union(Expression.EMPTY, a);
        Union emptyAB = new Union(emptyA, b);
        Union result = new Union(a, emptyAB);
        
        Expression simplified = Simplifier.simplify(result);
        
        // After empty elimination and absorption
        assertTrue(simplified instanceof Union, "Should be a union");
        Union simplifiedUnion = (Union) simplified;
        assertTrue(containsElement(simplifiedUnion, a), "Should contain a");
        assertTrue(containsElement(simplifiedUnion, b), "Should contain b");
    }
    
    @Test
    void testAbsorptionWithEpsilon() {
        // A | ((ε | A) | B) should simplify
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union epsilonA = new Union(Expression.EPSILON, a);
        Union epsilonAB = new Union(epsilonA, b);
        Union result = new Union(a, epsilonAB);
        
        Expression simplified = Simplifier.simplify(result);
        
        // A should be absorbed since it's in the nested union
        assertEquals(epsilonAB, simplified, "A | ((ε | A) | B) should simplify");
    }
    
    @Test
    void testNoFalseAbsorption() {
        // (A | B) | (C | D) - no absorption should happen
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        Token d = new Token("d");
        
        Union ab = new Union(a, b);
        Union cd = new Union(c, d);
        Union result = new Union(ab, cd);
        
        Expression simplified = Simplifier.simplify(result);
        
        assertTrue(simplified instanceof Union, "Should remain a union");
        // Both sides should be preserved
        Union simplifiedUnion = (Union) simplified;
        assertTrue(simplifiedUnion.lhs().equals(ab) || simplifiedUnion.lhs().equals(cd));
        assertTrue(simplifiedUnion.rhs().equals(ab) || simplifiedUnion.rhs().equals(cd));
    }
    
    @Test
    void testAbsorptionPreservesStructure() {
        // A | (A | (B | C)) should preserve (B | C) structure
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union bc = new Union(b, c);
        Union abc = new Union(a, bc);
        Union result = new Union(a, abc);
        
        Expression simplified = Simplifier.simplify(result);
        
        assertEquals(abc, simplified, "A | (A | (B | C)) should simplify to A | (B | C)");
        
        // Verify structure is preserved
        Union simplifiedUnion = (Union) simplified;
        assertEquals(a, simplifiedUnion.lhs());
        assertEquals(bc, simplifiedUnion.rhs());
    }
    
    @Test
    void testRecursiveAbsorptionBothSides() {
        // (A | B) | (C | (A | B)) - left is contained in right
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Union ab1 = new Union(a, b);
        Union ab2 = new Union(a, b);
        Union cab = new Union(c, ab2);
        Union result = new Union(ab1, cab);
        
        Expression simplified = Simplifier.simplify(result);
        assertEquals(cab, simplified, "(A | B) | (C | (A | B)) should simplify to C | (A | B)");
    }
    
    // Helper method to check if a union contains an element
    private boolean containsElement(Union union, Expression element) {
        if (union.lhs().equals(element) || union.rhs().equals(element)) {
            return true;
        }
        if (union.lhs() instanceof Union u) {
            if (containsElement(u, element)) return true;
        }
        if (union.rhs() instanceof Union u) {
            if (containsElement(u, element)) return true;
        }
        return false;
    }
}
