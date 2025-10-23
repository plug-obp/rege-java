package rege.semantics;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Inhabitation visitor.
 */
class InhabitationTest {
    
    private final Inhabitation inhabitation = new Inhabitation();
    
    @Test
    void testEmptyNotInhabited() {
        assertFalse(Expression.EMPTY.accept(inhabitation, null));
        assertFalse(Inhabitation.isInhabited(Expression.EMPTY));
    }
    
    @Test
    void testEpsilonIsInhabited() {
        assertTrue(Expression.EPSILON.accept(inhabitation, null));
        assertTrue(Inhabitation.isInhabited(Expression.EPSILON));
    }
    
    @Test
    void testTokenIsInhabited() {
        Expression token = new Token("a");
        assertTrue(token.accept(inhabitation, null));
        assertTrue(Inhabitation.isInhabited(token));
    }
    
    @Test
    void testConcatenationBothInhabited() {
        // a⋅b is inhabited
        Expression concat = new Concatenation(new Token("a"), new Token("b"));
        assertTrue(concat.accept(inhabitation, null));
    }
    
    @Test
    void testConcatenationLeftEmpty() {
        // ∅⋅a is not inhabited
        Expression concat = new Concatenation(Expression.EMPTY, new Token("a"));
        assertFalse(concat.accept(inhabitation, null));
    }
    
    @Test
    void testConcatenationRightEmpty() {
        // a⋅∅ is not inhabited
        Expression concat = new Concatenation(new Token("a"), Expression.EMPTY);
        assertFalse(concat.accept(inhabitation, null));
    }
    
    @Test
    void testConcatenationBothEmpty() {
        // ∅⋅∅ is not inhabited
        Expression concat = new Concatenation(Expression.EMPTY, Expression.EMPTY);
        assertFalse(concat.accept(inhabitation, null));
    }
    
    @Test
    void testConcatenationWithEpsilon() {
        // ε⋅a is inhabited
        Expression concat = new Concatenation(Expression.EPSILON, new Token("a"));
        assertTrue(concat.accept(inhabitation, null));
    }
    
    @Test
    void testUnionLeftInhabited() {
        // a|∅ is inhabited
        Expression union = new Union(new Token("a"), Expression.EMPTY);
        assertTrue(union.accept(inhabitation, null));
    }
    
    @Test
    void testUnionRightInhabited() {
        // ∅|a is inhabited
        Expression union = new Union(Expression.EMPTY, new Token("a"));
        assertTrue(union.accept(inhabitation, null));
    }
    
    @Test
    void testUnionBothInhabited() {
        // a|b is inhabited
        Expression union = new Union(new Token("a"), new Token("b"));
        assertTrue(union.accept(inhabitation, null));
    }
    
    @Test
    void testUnionNeitherInhabited() {
        // ∅|∅ is not inhabited
        Expression union = new Union(Expression.EMPTY, Expression.EMPTY);
        assertFalse(union.accept(inhabitation, null));
    }
    
    @Test
    void testKleeneStarAlwaysInhabited() {
        // a* is inhabited (contains "")
        Expression star = new KleeneStar(new Token("a"));
        assertTrue(star.accept(inhabitation, null));
        
        // ∅* is inhabited (contains "")
        Expression emptyStar = new KleeneStar(Expression.EMPTY);
        assertTrue(emptyStar.accept(inhabitation, null));
        
        // ε* is inhabited (contains "")
        Expression epsilonStar = new KleeneStar(Expression.EPSILON);
        assertTrue(epsilonStar.accept(inhabitation, null));
    }
    
    @Test
    void testComplexExpression1() {
        // (∅|a)⋅b is inhabited (a⋅b is in the language)
        Expression union = new Union(Expression.EMPTY, new Token("a"));
        Expression concat = new Concatenation(union, new Token("b"));
        assertTrue(concat.accept(inhabitation, null));
    }
    
    @Test
    void testComplexExpression2() {
        // (∅⋅a)* is inhabited (contains "")
        Expression concat = new Concatenation(Expression.EMPTY, new Token("a"));
        Expression star = new KleeneStar(concat);
        assertTrue(star.accept(inhabitation, null));
    }
    
    @Test
    void testComplexExpression3() {
        // ∅⋅∅* is not inhabited (left side is empty)
        Expression star = new KleeneStar(Expression.EMPTY);
        Expression concat = new Concatenation(Expression.EMPTY, star);
        assertFalse(concat.accept(inhabitation, null));
    }
    
    @Test
    void testDifferenceBetweenNullabilityAndInhabitation() {
        // a is inhabited but not nullable
        Expression a = new Token("a");
        assertTrue(Inhabitation.isInhabited(a));
        assertFalse(Nullability.isNullable(a));
        
        // ε is both inhabited and nullable
        assertTrue(Inhabitation.isInhabited(Expression.EPSILON));
        assertTrue(Nullability.isNullable(Expression.EPSILON));
        
        // ∅ is neither inhabited nor nullable
        assertFalse(Inhabitation.isInhabited(Expression.EMPTY));
        assertFalse(Nullability.isNullable(Expression.EMPTY));
        
        // a* is both inhabited and nullable
        Expression aStar = new KleeneStar(a);
        assertTrue(Inhabitation.isInhabited(aStar));
        assertTrue(Nullability.isNullable(aStar));
    }
}
