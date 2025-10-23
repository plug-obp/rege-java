package rege.syntax.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Simplifier visitor.
 */
class SimplifierTest {
    
    private static final Expression EMPTY = Expression.EMPTY;
    private static final Expression EPSILON = Expression.EPSILON;
    private final Simplifier simplifier = new Simplifier();
    
    // Helper method to simplify
    private Expression simplify(Expression expr) {
        return expr.accept(simplifier, null);
    }
    
    @Test
    void testTokenIsUnchanged() {
        Token a = new Token("a");
        assertSame(a, simplify(a));
    }
    
    @Test
    void testEmptyIsUnchanged() {
        assertSame(EMPTY, simplify(EMPTY));
    }
    
    @Test
    void testEpsilonIsUnchanged() {
        assertSame(EPSILON, simplify(EPSILON));
    }
    
    // Union Simplification Tests
    
    @Test
    void testUnionWithEmptyLeft() {
        // ∅ | a = a
        Token a = new Token("a");
        Union union = new Union(EMPTY, a);
        assertEquals(a, simplify(union));
    }
    
    @Test
    void testUnionWithEmptyRight() {
        // a | ∅ = a
        Token a = new Token("a");
        Union union = new Union(a, EMPTY);
        assertEquals(a, simplify(union));
    }
    
    @Test
    void testUnionIdempotent() {
        // a | a = a
        Token a = new Token("a");
        Union union = new Union(a, a);
        assertEquals(a, simplify(union));
    }
    
    @Test
    void testUnionDifferentTokensUnchanged() {
        // a | b = a | b (no simplification)
        Token a = new Token("a");
        Token b = new Token("b");
        Union union = new Union(a, b);
        Expression result = simplify(union);
        assertTrue(result instanceof Union);
        Union resultUnion = (Union) result;
        assertEquals(a, resultUnion.lhs());
        assertEquals(b, resultUnion.rhs());
    }
    
    @Test
    void testUnionNestedEmptySimplification() {
        // (∅ | a) | b = a | b
        Token a = new Token("a");
        Token b = new Token("b");
        Union inner = new Union(EMPTY, a);
        Union outer = new Union(inner, b);
        Expression result = simplify(outer);
        assertTrue(result instanceof Union);
        Union resultUnion = (Union) result;
        assertEquals(a, resultUnion.lhs());
        assertEquals(b, resultUnion.rhs());
    }
    
    // Concatenation Simplification Tests
    
    @Test
    void testConcatWithEmptyLeft() {
        // ∅a = ∅
        Token a = new Token("a");
        Concatenation concat = new Concatenation(EMPTY, a);
        assertSame(EMPTY, simplify(concat));
    }
    
    @Test
    void testConcatWithEmptyRight() {
        // a∅ = ∅
        Token a = new Token("a");
        Concatenation concat = new Concatenation(a, EMPTY);
        assertSame(EMPTY, simplify(concat));
    }
    
    @Test
    void testConcatWithEpsilonLeft() {
        // εa = a
        Token a = new Token("a");
        Concatenation concat = new Concatenation(EPSILON, a);
        assertEquals(a, simplify(concat));
    }
    
    @Test
    void testConcatWithEpsilonRight() {
        // aε = a
        Token a = new Token("a");
        Concatenation concat = new Concatenation(a, EPSILON);
        assertEquals(a, simplify(concat));
    }
    
    @Test
    void testConcatNormalCase() {
        // ab = ab (no simplification)
        Token a = new Token("a");
        Token b = new Token("b");
        Concatenation concat = new Concatenation(a, b);
        Expression result = simplify(concat);
        assertTrue(result instanceof Concatenation);
        Concatenation resultConcat = (Concatenation) result;
        assertEquals(a, resultConcat.lhs());
        assertEquals(b, resultConcat.rhs());
    }
    
    @Test
    void testConcatNestedEpsilonSimplification() {
        // (εa)b = ab
        Token a = new Token("a");
        Token b = new Token("b");
        Concatenation inner = new Concatenation(EPSILON, a);
        Concatenation outer = new Concatenation(inner, b);
        Expression result = simplify(outer);
        assertTrue(result instanceof Concatenation);
        Concatenation resultConcat = (Concatenation) result;
        assertEquals(a, resultConcat.lhs());
        assertEquals(b, resultConcat.rhs());
    }
    
    // Kleene Star Simplification Tests
    
    @Test
    void testStarOfEmpty() {
        // ∅* = ε
        KleeneStar star = new KleeneStar(EMPTY);
        assertSame(EPSILON, simplify(star));
    }
    
    @Test
    void testStarOfEpsilon() {
        // ε* = ε
        KleeneStar star = new KleeneStar(EPSILON);
        assertSame(EPSILON, simplify(star));
    }
    
    @Test
    void testStarOfStar() {
        // (a*)* = a*
        Token a = new Token("a");
        KleeneStar inner = new KleeneStar(a);
        KleeneStar outer = new KleeneStar(inner);
        Expression result = simplify(outer);
        assertEquals(inner, result);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertEquals(a, resultStar.expression());
    }
    
    @Test
    void testStarOfToken() {
        // a* = a* (no simplification)
        Token a = new Token("a");
        KleeneStar star = new KleeneStar(a);
        Expression result = simplify(star);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertEquals(a, resultStar.expression());
    }
    
    @Test
    void testTripleNestedStar() {
        // ((a*)*)* = a*
        Token a = new Token("a");
        KleeneStar star1 = new KleeneStar(a);
        KleeneStar star2 = new KleeneStar(star1);
        KleeneStar star3 = new KleeneStar(star2);
        Expression result = simplify(star3);
        assertEquals(star1, result);
    }
    
    // Complex Nested Simplification Tests
    
    @Test
    void testComplexNestedSimplification1() {
        // (∅ | a)* = a*
        Token a = new Token("a");
        Union union = new Union(EMPTY, a);
        KleeneStar star = new KleeneStar(union);
        Expression result = simplify(star);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertEquals(a, resultStar.expression());
    }
    
    @Test
    void testComplexNestedSimplification2() {
        // (εa)* = a*
        Token a = new Token("a");
        Concatenation concat = new Concatenation(EPSILON, a);
        KleeneStar star = new KleeneStar(concat);
        Expression result = simplify(star);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertEquals(a, resultStar.expression());
    }
    
    @Test
    void testComplexNestedSimplification3() {
        // (a∅) | b = ∅ | b = b
        Token a = new Token("a");
        Token b = new Token("b");
        Concatenation concat = new Concatenation(a, EMPTY);
        Union union = new Union(concat, b);
        Expression result = simplify(union);
        assertEquals(b, result);
    }
    
    @Test
    void testComplexNestedSimplification4() {
        // ((a | ∅)b)* = (ab)*
        Token a = new Token("a");
        Token b = new Token("b");
        Union union = new Union(a, EMPTY);
        Concatenation concat = new Concatenation(union, b);
        KleeneStar star = new KleeneStar(concat);
        Expression result = simplify(star);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertTrue(resultStar.expression() instanceof Concatenation);
        Concatenation resultConcat = (Concatenation) resultStar.expression();
        assertEquals(a, resultConcat.lhs());
        assertEquals(b, resultConcat.rhs());
    }
    
    @Test
    void testStaticSimplifyMethod() {
        // Test the static convenience method
        Token a = new Token("a");
        KleeneStar inner = new KleeneStar(a);
        KleeneStar outer = new KleeneStar(inner);
        Expression result = Simplifier.simplify(outer);
        assertEquals(inner, result);
    }
    
    @Test
    void testDeeplyNestedExpression() {
        // ((ε | (a∅))b)* = (εb)* = b*
        Token a = new Token("a");
        Token b = new Token("b");
        Concatenation emptyConcat = new Concatenation(a, EMPTY);
        Union union = new Union(EPSILON, emptyConcat);
        Concatenation concat = new Concatenation(union, b);
        KleeneStar star = new KleeneStar(concat);
        
        Expression result = simplify(star);
        assertTrue(result instanceof KleeneStar);
        KleeneStar resultStar = (KleeneStar) result;
        assertEquals(b, resultStar.expression());
    }
}
