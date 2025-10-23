package rege.semantics;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Brzozowski derivatives.
 */
class BrzozowskiTest {
    
    // Simple character-based evaluator: token matches if it equals the character
    private final BiPredicate<String, Character> charEvaluator = 
        (token, ch) -> token.equals(String.valueOf(ch));
    
    private final Brzozowski<Character> brz = new Brzozowski<>(charEvaluator);
    
    // ========== Basic Derivatives ==========
    
    @Test
    void testDerivativeOfEmpty() {
        // D_a(∅) = ∅
        Expression result = brz.derivative(Expression.EMPTY, 'a');
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testDerivativeOfEpsilon() {
        // D_a(ε) = ∅
        Expression result = brz.derivative(Expression.EPSILON, 'a');
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testDerivativeOfMatchingToken() {
        // D_a(τ[a]) = ε
        Expression token = new Token("a");
        Expression result = brz.derivative(token, 'a');
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testDerivativeOfNonMatchingToken() {
        // D_a(τ[b]) = ∅
        Expression token = new Token("b");
        Expression result = brz.derivative(token, 'a');
        assertEquals(Expression.EMPTY, result);
    }
    
    // ========== Union Derivatives ==========
    
    @Test
    void testDerivativeOfUnion() {
        // D_a(a|b) = D_a(a) | D_a(b) = ε | ∅ = ε
        Expression union = new Union(new Token("a"), new Token("b"));
        Expression result = brz.derivative(union, 'a');
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testDerivativeOfUnionNoMatch() {
        // D_c(a|b) = ∅ | ∅ = ∅
        Expression union = new Union(new Token("a"), new Token("b"));
        Expression result = brz.derivative(union, 'c');
        assertEquals(Expression.EMPTY, result);
    }
    
    // ========== Concatenation Derivatives ==========
    
    @Test
    void testDerivativeOfConcatenationLeftNotNullable() {
        // D_a(a⋅b) = D_a(a)⋅b = ε⋅b = b
        Expression concat = new Concatenation(new Token("a"), new Token("b"));
        Expression result = brz.derivative(concat, 'a');
        assertEquals(new Token("b"), result);
    }
    
    @Test
    void testDerivativeOfConcatenationLeftNullable() {
        // D_a(ε⋅a) = D_a(ε)⋅a | D_a(a) = ∅⋅a | ε = ε
        Expression concat = new Concatenation(Expression.EPSILON, new Token("a"));
        Expression result = brz.derivative(concat, 'a');
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testDerivativeOfConcatenationLeftNullableComplex() {
        // D_a((a|ε)⋅b) = D_a(a|ε)⋅b | D_a(b) = ε⋅b | ∅ = b
        Expression union = new Union(new Token("a"), Expression.EPSILON);
        Expression concat = new Concatenation(union, new Token("b"));
        Expression result = brz.derivative(concat, 'a');
        assertEquals(new Token("b"), result);
    }
    
    // ========== Kleene Star Derivatives ==========
    
    @Test
    void testDerivativeOfKleeneStar() {
        // D_a(a*) = D_a(a)⋅a* = ε⋅a* = a*
        Expression star = new KleeneStar(new Token("a"));
        Expression result = brz.derivative(star, 'a');
        assertEquals(star, result);
    }
    
    @Test
    void testDerivativeOfKleeneStarNoMatch() {
        // D_b(a*) = D_b(a)⋅a* = ∅⋅a* = ∅
        Expression star = new KleeneStar(new Token("a"));
        Expression result = brz.derivative(star, 'b');
        assertEquals(Expression.EMPTY, result);
    }
    
    // ========== String Matching ==========
    
    @Test
    void testMatchSingleCharacter() {
        Expression pattern = new Token("a");
        assertTrue(brz.matches(pattern, List.of('a')));
        assertFalse(brz.matches(pattern, List.of('b')));
        assertFalse(brz.matches(pattern, List.of()));
    }
    
    @Test
    void testMatchConcatenation() {
        // Pattern: a⋅b
        Expression pattern = new Concatenation(new Token("a"), new Token("b"));
        assertTrue(brz.matches(pattern, List.of('a', 'b')));
        assertFalse(brz.matches(pattern, List.of('a')));
        assertFalse(brz.matches(pattern, List.of('b')));
        assertFalse(brz.matches(pattern, List.of('a', 'b', 'c')));
    }
    
    @Test
    void testMatchUnion() {
        // Pattern: a|b
        Expression pattern = new Union(new Token("a"), new Token("b"));
        assertTrue(brz.matches(pattern, List.of('a')));
        assertTrue(brz.matches(pattern, List.of('b')));
        assertFalse(brz.matches(pattern, List.of('c')));
        assertFalse(brz.matches(pattern, List.of('a', 'b')));
    }
    
    @Test
    void testMatchKleeneStar() {
        // Pattern: a*
        Expression pattern = new KleeneStar(new Token("a"));
        assertTrue(brz.matches(pattern, List.of()));
        assertTrue(brz.matches(pattern, List.of('a')));
        assertTrue(brz.matches(pattern, List.of('a', 'a')));
        assertTrue(brz.matches(pattern, List.of('a', 'a', 'a')));
        assertFalse(brz.matches(pattern, List.of('b')));
        assertFalse(brz.matches(pattern, List.of('a', 'b')));
    }
    
    @Test
    void testMatchComplexPattern1() {
        // Pattern: (a|b)*
        Expression union = new Union(new Token("a"), new Token("b"));
        Expression pattern = new KleeneStar(union);
        
        assertTrue(brz.matches(pattern, List.of()));
        assertTrue(brz.matches(pattern, List.of('a')));
        assertTrue(brz.matches(pattern, List.of('b')));
        assertTrue(brz.matches(pattern, List.of('a', 'b')));
        assertTrue(brz.matches(pattern, List.of('b', 'a')));
        assertTrue(brz.matches(pattern, List.of('a', 'a', 'b', 'b')));
        assertFalse(brz.matches(pattern, List.of('c')));
        assertFalse(brz.matches(pattern, List.of('a', 'b', 'c')));
    }
    
    @Test
    void testMatchComplexPattern2() {
        // Pattern: (a|b)*⋅c
        Expression union = new Union(new Token("a"), new Token("b"));
        Expression star = new KleeneStar(union);
        Expression pattern = new Concatenation(star, new Token("c"));
        
        assertTrue(brz.matches(pattern, List.of('c')));
        assertTrue(brz.matches(pattern, List.of('a', 'c')));
        assertTrue(brz.matches(pattern, List.of('b', 'c')));
        assertTrue(brz.matches(pattern, List.of('a', 'b', 'c')));
        assertFalse(brz.matches(pattern, List.of()));
        assertFalse(brz.matches(pattern, List.of('a')));
        assertFalse(brz.matches(pattern, List.of('c', 'a')));
    }
    
    @Test
    void testMatchComplexPattern3() {
        // Pattern: a*⋅b*
        Expression starA = new KleeneStar(new Token("a"));
        Expression starB = new KleeneStar(new Token("b"));
        Expression pattern = new Concatenation(starA, starB);
        
        assertTrue(brz.matches(pattern, List.of()));
        assertTrue(brz.matches(pattern, List.of('a')));
        assertTrue(brz.matches(pattern, List.of('b')));
        assertTrue(brz.matches(pattern, List.of('a', 'b')));
        assertTrue(brz.matches(pattern, List.of('a', 'a', 'b', 'b')));
        assertFalse(brz.matches(pattern, List.of('b', 'a'))); // order matters!
        assertFalse(brz.matches(pattern, List.of('a', 'b', 'a')));
    }
    
    // ========== Successive Derivatives ==========
    
    @Test
    void testSuccessiveDerivatives() {
        // Pattern: a⋅b⋅c
        Expression ab = new Concatenation(new Token("a"), new Token("b"));
        Expression pattern = new Concatenation(ab, new Token("c"));
        
        // D_a(a⋅b⋅c) = b⋅c
        Expression d1 = brz.derivative(pattern, 'a');
        assertFalse(brz.isNullable(d1));
        
        // D_b(b⋅c) = c
        Expression d2 = brz.derivative(d1, 'b');
        assertFalse(brz.isNullable(d2));
        
        // D_c(c) = ε
        Expression d3 = brz.derivative(d2, 'c');
        assertTrue(brz.isNullable(d3));
        assertEquals(Expression.EPSILON, d3);
    }
    
    @Test
    void testDerivativeSequenceWithStar() {
        // Pattern: (ab)*
        Expression ab = new Concatenation(new Token("a"), new Token("b"));
        Expression pattern = new KleeneStar(ab);
        
        // Initially nullable (zero repetitions)
        assertTrue(brz.isNullable(pattern));
        
        // D_a((ab)*) = D_a(ab)⋅(ab)* = b⋅(ab)*
        Expression d1 = brz.derivative(pattern, 'a');
        assertFalse(brz.isNullable(d1));
        
        // D_b(b⋅(ab)*) = (ab)*
        Expression d2 = brz.derivative(d1, 'b');
        assertTrue(brz.isNullable(d2));
    }
    
    // ========== Custom Evaluator ==========
    
    @Test
    void testCustomEvaluator() {
        // Evaluator that matches tokens containing the character
        BiPredicate<String, Character> containsEvaluator = 
            (token, ch) -> token.contains(String.valueOf(ch));
        
        Brzozowski<Character> customBrz = new Brzozowski<>(containsEvaluator);
        
        Expression pattern = new Token("abc");
        
        // All of 'a', 'b', 'c' match because token contains them
        assertEquals(Expression.EPSILON, customBrz.derivative(pattern, 'a'));
        assertEquals(Expression.EPSILON, customBrz.derivative(pattern, 'b'));
        assertEquals(Expression.EPSILON, customBrz.derivative(pattern, 'c'));
        assertEquals(Expression.EMPTY, customBrz.derivative(pattern, 'd'));
    }
    
    @Test
    void testCaseInsensitiveEvaluator() {
        // Case-insensitive evaluator
        BiPredicate<String, Character> caseInsensitive = 
            (token, ch) -> token.equalsIgnoreCase(String.valueOf(ch));
        
        Brzozowski<Character> customBrz = new Brzozowski<>(caseInsensitive);
        
        Expression pattern = new Token("a");
        
        assertEquals(Expression.EPSILON, customBrz.derivative(pattern, 'a'));
        assertEquals(Expression.EPSILON, customBrz.derivative(pattern, 'A'));
        assertEquals(Expression.EMPTY, customBrz.derivative(pattern, 'b'));
    }
}
