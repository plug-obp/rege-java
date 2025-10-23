package rege.syntax.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for absorption laws in the union factory method.
 */
class UnionAbsorptionTest {
    
    @Test
    void testAbsorptionLeftIntoRight() {
        // A | (A | B) = A | B
        Token a = new Token("a");
        Token b = new Token("b");
        
        Expression ab = a.union(b);  // A | B
        Expression result = a.union(ab);  // A | (A | B)
        
        assertEquals(ab, result, "A | (A | B) should equal A | B (absorption)");
    }
    
    @Test
    void testAbsorptionRightIntoLeft() {
        // (A | B) | A = A | B
        Token a = new Token("a");
        Token b = new Token("b");
        
        Expression ab = a.union(b);  // A | B
        Expression result = ab.union(a);  // (A | B) | A
        
        assertEquals(ab, result, "(A | B) | A should equal A | B (absorption)");
    }
    
    @Test
    void testAbsorptionWithCommutativeUnion() {
        // A | (B | A) = B | A (or A | B depending on order)
        Token a = new Token("a");
        Token b = new Token("b");
        
        Expression ba = b.union(a);  // B | A
        Expression result = a.union(ba);  // A | (B | A)
        
        assertEquals(ba, result, "A | (B | A) should equal B | A (absorption)");
    }
    
    @Test
    void testAbsorptionRightSide() {
        // (B | A) | A = B | A
        Token a = new Token("a");
        Token b = new Token("b");
        
        Expression ba = b.union(a);  // B | A
        Expression result = ba.union(a);  // (B | A) | A
        
        assertEquals(ba, result, "(B | A) | A should equal B | A (absorption)");
    }
    
    @Test
    void testNoAbsorptionWhenDifferent() {
        // A | (B | C) where A, B, C are different → no simplification
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Expression bc = b.union(c);  // B | C
        Expression result = a.union(bc);  // A | (B | C)
        
        assertTrue(result instanceof Union, "Should create new union when no absorption applies");
        assertNotEquals(bc, result, "A | (B | C) should not simplify to B | C");
    }
    
    @Test
    void testNestedAbsorption() {
        // A | ((A | B) | C) - should detect A in the nested union
        // Note: Our simple absorption only checks one level deep
        // This actually creates: A | (Union(Union(A, B), C))
        // Since (A | B) | C is a Union where lhs = Union(A, B), not just A,
        // the simple check won't catch it
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Expression ab = a.union(b);  // A | B
        Expression abc = ab.union(c);  // (A | B) | C
        Expression result = a.union(abc);  // A | ((A | B) | C)
        
        // With simple absorption, this doesn't fully simplify
        // because (A|B)|C doesn't directly contain A at the top level
        assertTrue(result instanceof Union, 
            "Nested absorption requires recursive simplification");
    }
    
    @Test
    void testComplexAbsorption() {
        // (A | B) | (A | C) - both contain A
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        Expression ab = a.union(b);  // A | B
        Expression ac = a.union(c);  // A | C
        Expression result = ab.union(ac);  // (A | B) | (A | C)
        
        // No direct absorption here since neither union equals just A
        // This would need deeper simplification (beyond absorption)
        assertTrue(result instanceof Union, "Complex case creates union");
    }
    
    @Test
    void testAbsorptionPreservesOtherElements() {
        // B | (A | B) = A | B (B is absorbed, A remains)
        Token a = new Token("a");
        Token b = new Token("b");
        
        Expression ab = a.union(b);  // A | B
        Expression result = b.union(ab);  // B | (A | B)
        
        assertEquals(ab, result, "B | (A | B) should equal A | B");
        
        // Verify the structure
        if (result instanceof Union u) {
            assertTrue(
                (u.lhs().equals(a) && u.rhs().equals(b)) ||
                (u.lhs().equals(b) && u.rhs().equals(a)),
                "Result should contain both A and B"
            );
        }
    }
    
    @Test
    void testAbsorptionWithEmpty() {
        // A | (∅ | A) = A (empty is eliminated first, then idempotent)
        Token a = new Token("a");
        
        Expression emptyA = Expression.EMPTY.union(a);  // ∅ | A = A
        Expression result = a.union(emptyA);  // A | A
        
        assertEquals(a, result, "A | (∅ | A) should simplify to A");
    }
    
    @Test
    void testAbsorptionWithEpsilon() {
        // A | (ε | A) = A | ε or simplified based on structure
        Token a = new Token("a");
        
        Expression epsilonA = Expression.EPSILON.union(a);  // ε | A
        Expression result = a.union(epsilonA);  // A | (ε | A)
        
        // Since A is in (ε | A), absorption should return (ε | A)
        assertEquals(epsilonA, result, "A | (ε | A) should equal ε | A (absorption)");
    }
}
