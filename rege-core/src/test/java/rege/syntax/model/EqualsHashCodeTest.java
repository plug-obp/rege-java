package rege.syntax.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying the correctness of equals and hashCode implementations.
 */
class EqualsHashCodeTest {
    
    @Test
    void testUnionCommutativityEquals() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union u1 = new Union(a, b);  // a | b
        Union u2 = new Union(b, a);  // b | a
        
        // Commutativity: a | b should equal b | a
        assertEquals(u1, u2, "Union should be commutative in equals()");
    }
    
    @Test
    void testUnionCommutativityHashCode() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union u1 = new Union(a, b);  // a | b
        Union u2 = new Union(b, a);  // b | a
        
        // Critical: if equals returns true, hashCode must be the same!
        assertEquals(u1.hashCode(), u2.hashCode(), 
            "Union hashCode must be the same for commutative operands");
    }
    
    @Test
    void testUnionInHashSet() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union u1 = new Union(a, b);
        Union u2 = new Union(b, a);
        
        HashSet<Expression> set = new HashSet<>();
        set.add(u1);
        
        // Since u1.equals(u2) and hashCodes match, set should recognize u2 as duplicate
        assertTrue(set.contains(u2), "HashSet should recognize commutative unions as same");
        assertFalse(set.add(u2), "Should not add duplicate commutative union");
        assertEquals(1, set.size(), "HashSet should contain only one element");
    }
    
    @Test
    void testUnionInHashMap() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        Union u1 = new Union(a, b);
        Union u2 = new Union(b, a);
        
        HashMap<Expression, String> map = new HashMap<>();
        map.put(u1, "first");
        
        // u2 should map to the same key as u1
        assertTrue(map.containsKey(u2), "HashMap should recognize commutative unions as same key");
        assertEquals("first", map.get(u2), "Should retrieve same value for commutative union");
        
        // Overwriting with u2 should replace u1's value
        map.put(u2, "second");
        assertEquals(1, map.size(), "HashMap should still have only one entry");
        assertEquals("second", map.get(u1), "Value should be updated");
    }
    
    @Test
    void testUnionCanonicalConstruction() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        // Using union() method should create canonical form
        Expression u1 = a.union(b);
        Expression u2 = b.union(a);
        
        // Should be the exact same object or at least equal
        assertEquals(u1, u2, "union() should produce equal results regardless of order");
        
        // Check if they're canonically ordered
        assertTrue(u1 instanceof Union);
        Union union = (Union) u1;
        
        // The one with smaller hashCode should be on left
        if (a.hashCode() < b.hashCode()) {
            assertEquals(a, union.lhs());
            assertEquals(b, union.rhs());
        } else {
            assertEquals(b, union.lhs());
            assertEquals(a, union.rhs());
        }
    }
    
    @Test
    void testSingletonHashCodesAreConstant() {
        Expression e1 = Expression.EMPTY;
        Empty e2 = Empty.instance();
        
        assertEquals(e1.hashCode(), e2.hashCode(), "Singleton hashCode must be constant");
        // assertEquals(37, e2.hashCode(), "Empty hashCode should be 37");
        
        Expression eps1 = Expression.EPSILON;
        Epsilon eps2 = Epsilon.instance();
        
        assertEquals(eps1.hashCode(), eps2.hashCode(), "Singleton hashCode must be constant");
        // assertEquals(31, eps2.hashCode(), "Epsilon hashCode should be 31");
    }
    
    @Test
    void testSingletonEquality() {
        Expression e1 = Expression.EMPTY;
        Empty e2 = Empty.instance();
        
        assertSame(e1, e2, "Empty should be singleton");
        assertEquals(e1, e2, "Empty instances should be equal");
        assertTrue(e1 == e2, "Empty should use identity equality");
        
        Expression eps1 = Expression.EPSILON;
        Epsilon eps2 = Epsilon.instance();
        
        assertSame(eps1, eps2, "Epsilon should be singleton");
        assertEquals(eps1, eps2, "Epsilon instances should be equal");
        assertTrue(eps1 == eps2, "Epsilon should use identity equality");
    }
    
    @Test
    void testTokenEquality() {
        Token a1 = new Token("a");
        Token a2 = new Token("a");
        Token b = new Token("b");
        
        assertEquals(a1, a2, "Tokens with same value should be equal");
        assertNotEquals(a1, b, "Tokens with different values should not be equal");
        
        assertEquals(a1.hashCode(), a2.hashCode(), "Equal tokens should have same hashCode");
    }
    
    @Test
    void testConcatenationEquality() {
        Token a = new Token("a");
        Token b = new Token("b");
        
        Concatenation c1 = new Concatenation(a, b);
        Concatenation c2 = new Concatenation(a, b);
        Concatenation c3 = new Concatenation(b, a);
        
        assertEquals(c1, c2, "Concatenations with same operands should be equal");
        assertNotEquals(c1, c3, "Concatenation is not commutative");
        
        assertEquals(c1.hashCode(), c2.hashCode(), "Equal concatenations should have same hashCode");
    }
    
    @Test
    void testKleeneStarEquality() {
        Token a = new Token("a");
        
        KleeneStar k1 = new KleeneStar(a);
        KleeneStar k2 = new KleeneStar(a);
        KleeneStar k3 = new KleeneStar(new Token("a"));
        
        assertEquals(k1, k2, "KleeneStars with same expression should be equal");
        assertEquals(k1, k3, "KleeneStars with equal expressions should be equal");
        
        assertEquals(k1.hashCode(), k2.hashCode(), "Equal KleeneStars should have same hashCode");
        assertEquals(k1.hashCode(), k3.hashCode(), "Equal KleeneStars should have same hashCode");
    }
    
    @Test
    void testComplexUnionHashCode() {
        Token a = new Token("a");
        Token b = new Token("b");
        Token c = new Token("c");
        
        // (a | b) | c  vs  (b | a) | c
        Union inner1 = new Union(a, b);
        Union inner2 = new Union(b, a);
        Union outer1 = new Union(inner1, c);
        Union outer2 = new Union(inner2, c);
        
        // inner1 and inner2 should be equal and have same hashCode
        assertEquals(inner1, inner2);
        assertEquals(inner1.hashCode(), inner2.hashCode());
        
        // Therefore outer1 and outer2 should be equal and have same hashCode
        assertEquals(outer1, outer2);
        assertEquals(outer1.hashCode(), outer2.hashCode());
    }
    
    @Test
    void testUnionHashCodeNotZero() {
        Token a = new Token("a");
        Token b = new Token("b");
        Union u = new Union(a, b);
        
        // Hash code should not be zero (unlikely but possible edge case)
        // This is not strictly required, but zero hash is inefficient
        assertNotEquals(0, u.hashCode(), "Union hashCode should ideally not be zero");
    }
    
    @Test
    void testHashCodeConsistency() {
        Token a = new Token("a");
        Token b = new Token("b");
        Union u = new Union(a, b);
        
        int hash1 = u.hashCode();
        int hash2 = u.hashCode();
        
        assertEquals(hash1, hash2, "hashCode must be consistent across multiple calls");
    }
    
    // ========== Token Empty Value Tests ==========
    
    @Test
    void testTokenRejectsEmptyString() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Token(""),
            "Token should reject empty string"
        );
        
        assertTrue(
            exception.getMessage().contains("empty"),
            "Exception message should mention 'empty'"
        );
        assertTrue(
            exception.getMessage().contains("EPSILON"),
            "Exception message should suggest using EPSILON"
        );
    }
    
    @Test
    void testTokenAcceptsNonEmptyStrings() {
        // All of these should be valid
        assertDoesNotThrow(() -> new Token("a"));
        assertDoesNotThrow(() -> new Token("hello"));
        assertDoesNotThrow(() -> new Token(" "));  // Space is valid
        assertDoesNotThrow(() -> new Token("\n"));  // Newline is valid
        assertDoesNotThrow(() -> new Token("\t"));  // Tab is valid
        assertDoesNotThrow(() -> new Token("123"));  // Numbers are valid
    }
    
    @Test
    void testTokenRejectsNull() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new Token(null),
            "Token should reject null value"
        );
        
        assertTrue(
            exception.getMessage().contains("null"),
            "Exception message should mention 'null'"
        );
    }
}
