package rege.semantics;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Nullability visitor.
 */
class NullabilityTest {
    
    private final Nullability nullability = new Nullability();
    
    @Test
    void testEmptyNotNullable() {
        assertFalse(Expression.EMPTY.accept(nullability, null));
        assertFalse(Nullability.isNullable(Expression.EMPTY));
    }
    
    @Test
    void testEpsilonIsNullable() {
        assertTrue(Expression.EPSILON.accept(nullability, null));
        assertTrue(Nullability.isNullable(Expression.EPSILON));
    }
    
    @Test
    void testTokenNotNullable() {
        Expression token = new Token("a");
        assertFalse(token.accept(nullability, null));
        assertFalse(Nullability.isNullable(token));
    }
    
    @Test
    void testConcatenationBothNullable() {
        // ε⋅ε is nullable
        Expression concat = new Concatenation(Expression.EPSILON, Expression.EPSILON);
        assertTrue(concat.accept(nullability, null));
    }
    
    @Test
    void testConcatenationLeftNotNullable() {
        // a⋅ε is not nullable
        Expression concat = new Concatenation(new Token("a"), Expression.EPSILON);
        assertFalse(concat.accept(nullability, null));
    }
    
    @Test
    void testConcatenationRightNotNullable() {
        // ε⋅a is not nullable
        Expression concat = new Concatenation(Expression.EPSILON, new Token("a"));
        assertFalse(concat.accept(nullability, null));
    }
    
    @Test
    void testConcatenationNeitherNullable() {
        // a⋅b is not nullable
        Expression concat = new Concatenation(new Token("a"), new Token("b"));
        assertFalse(concat.accept(nullability, null));
    }
    
    @Test
    void testUnionLeftNullable() {
        // ε|a is nullable
        Expression union = new Union(Expression.EPSILON, new Token("a"));
        assertTrue(union.accept(nullability, null));
    }
    
    @Test
    void testUnionRightNullable() {
        // a|ε is nullable
        Expression union = new Union(new Token("a"), Expression.EPSILON);
        assertTrue(union.accept(nullability, null));
    }
    
    @Test
    void testUnionBothNullable() {
        // ε|ε is nullable
        Expression union = new Union(Expression.EPSILON, Expression.EPSILON);
        assertTrue(union.accept(nullability, null));
    }
    
    @Test
    void testUnionNeitherNullable() {
        // a|b is not nullable
        Expression union = new Union(new Token("a"), new Token("b"));
        assertFalse(union.accept(nullability, null));
    }
    
    @Test
    void testKleeneStarAlwaysNullable() {
        // a* is nullable
        Expression star = new KleeneStar(new Token("a"));
        assertTrue(star.accept(nullability, null));
        
        // ∅* is nullable
        Expression emptyStar = new KleeneStar(Expression.EMPTY);
        assertTrue(emptyStar.accept(nullability, null));
        
        // ε* is nullable
        Expression epsilonStar = new KleeneStar(Expression.EPSILON);
        assertTrue(epsilonStar.accept(nullability, null));
    }
    
    @Test
    void testComplexExpression1() {
        // (a|ε)⋅b is not nullable (right side requires 'b')
        Expression union = new Union(new Token("a"), Expression.EPSILON);
        Expression concat = new Concatenation(union, new Token("b"));
        assertFalse(concat.accept(nullability, null));
    }
    
    @Test
    void testComplexExpression2() {
        // (a|ε)⋅(b|ε) is nullable (both sides can be ε)
        Expression leftUnion = new Union(new Token("a"), Expression.EPSILON);
        Expression rightUnion = new Union(new Token("b"), Expression.EPSILON);
        Expression concat = new Concatenation(leftUnion, rightUnion);
        assertTrue(concat.accept(nullability, null));
    }
    
    @Test
    void testComplexExpression3() {
        // (a⋅b)* is nullable (star is always nullable)
        Expression concat = new Concatenation(new Token("a"), new Token("b"));
        Expression star = new KleeneStar(concat);
        assertTrue(star.accept(nullability, null));
    }
    
    @Test
    void testComplexExpression4() {
        // a|b|ε is nullable
        Expression union1 = new Union(new Token("a"), new Token("b"));
        Expression union2 = new Union(union1, Expression.EPSILON);
        assertTrue(union2.accept(nullability, null));
    }
    
    @Test
    void testComplexExpression5() {
        // a*⋅b*⋅c* is nullable (all stars are nullable)
        Expression starA = new KleeneStar(new Token("a"));
        Expression starB = new KleeneStar(new Token("b"));
        Expression starC = new KleeneStar(new Token("c"));
        Expression concat1 = new Concatenation(starA, starB);
        Expression concat2 = new Concatenation(concat1, starC);
        assertTrue(concat2.accept(nullability, null));
    }
}
