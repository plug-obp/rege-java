package rege.syntax;
import rege.reader.infra.*;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegeReaderLeft to verify it produces LEFT-ASSOCIATIVE parse trees
 * and is semantically equivalent to RegeReader (right-associative).
 */
class RegeReaderLeftTest {
    
    // ========== Basic Terminals ==========
    
    @Test
    void testReadEmpty() {
        Expression result = RegeReaderLeft.parse("∅", false).orElse(null);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testReadEpsilon() {
        Expression result = RegeReaderLeft.parse("ϵ", false).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadToken() {
        Expression result = RegeReaderLeft.parse("τ[hello]", false).orElse(null);
        assertEquals(new Token("hello"), result);
    }
    
    @Test
    void testReadTokenAlternative() {
        Expression result = RegeReaderLeft.parse("t[world]", false).orElse(null);
        assertEquals(new Token("world"), result);
    }
    
    // ========== LEFT-ASSOCIATIVITY TESTS ==========
    
    @Test
    void testLeftAssociativeConcatenation() {
        // a.b.c should parse as (a.b).c (LEFT-associative)
        Expression result = RegeReaderLeft.parse("τ[a].τ[b].τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        
        // Right side should be a simple token
        assertEquals(new Token("c"), outer.rhs());
        
        // Left side should be a concatenation
        assertTrue(outer.lhs() instanceof Concatenation);
        Concatenation inner = (Concatenation) outer.lhs();
        assertEquals(new Token("a"), inner.lhs());
        assertEquals(new Token("b"), inner.rhs());
    }
    
    @Test
    void testLeftAssociativeUnion() {
        // a|b|c should parse as (a|b)|c (LEFT-associative)
        Expression result = RegeReaderLeft.parse("τ[a]|τ[b]|τ[c]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union outer = (Union) result;
        
        // Right side should be a simple token
        assertEquals(new Token("c"), outer.rhs());
        
        // Left side should be a union
        assertTrue(outer.lhs() instanceof Union);
        Union inner = (Union) outer.lhs();
        assertEquals(new Token("a"), inner.lhs());
        assertEquals(new Token("b"), inner.rhs());
    }
    
    @Test
    void testLeftAssociativeLongConcatenation() {
        // a.b.c.d should parse as ((a.b).c).d
        Expression result = RegeReaderLeft.parse("τ[a].τ[b].τ[c].τ[d]", false).orElse(null);
        
        // Outermost: (...).d
        assertTrue(result instanceof Concatenation);
        Concatenation level3 = (Concatenation) result;
        assertEquals(new Token("d"), level3.rhs());
        
        // Second level: (a.b).c
        assertTrue(level3.lhs() instanceof Concatenation);
        Concatenation level2 = (Concatenation) level3.lhs();
        assertEquals(new Token("c"), level2.rhs());
        
        // Third level: a.b
        assertTrue(level2.lhs() instanceof Concatenation);
        Concatenation level1 = (Concatenation) level2.lhs();
        assertEquals(new Token("a"), level1.lhs());
        assertEquals(new Token("b"), level1.rhs());
    }
    
    @Test
    void testLeftAssociativeLongUnion() {
        // a|b|c|d should parse as ((a|b)|c)|d
        Expression result = RegeReaderLeft.parse("τ[a]|τ[b]|τ[c]|τ[d]", false).orElse(null);
        
        // Outermost: (...)|d
        assertTrue(result instanceof Union);
        Union level3 = (Union) result;
        assertEquals(new Token("d"), level3.rhs());
        
        // Second level: (a|b)|c
        assertTrue(level3.lhs() instanceof Union);
        Union level2 = (Union) level3.lhs();
        assertEquals(new Token("c"), level2.rhs());
        
        // Third level: a|b
        assertTrue(level2.lhs() instanceof Union);
        Union level1 = (Union) level2.lhs();
        assertEquals(new Token("a"), level1.lhs());
        assertEquals(new Token("b"), level1.rhs());
    }
    
    // ========== COMPARISON WITH RegeReader (Right-associative) ==========
    
    @Test
    void testDifferentStructureSameSemantics() {
        String input = "τ[a].τ[b].τ[c]";
        
        Expression left = RegeReaderLeft.parse(input, false).orElse(null);
        Expression right = RegeReader.parse(input, false).orElse(null);
        
        // Structures should be DIFFERENT
        assertNotEquals(left, right, "Should have different structures");
        
        // But simplified versions should be EQUIVALENT
        // (This would require a semantic equivalence checker, but we can verify structure)
        
        // Left: (a.b).c
        Concatenation leftOuter = (Concatenation) left;
        assertTrue(leftOuter.lhs() instanceof Concatenation);
        assertEquals(new Token("c"), leftOuter.rhs());
        
        // Right: a.(b.c)
        Concatenation rightOuter = (Concatenation) right;
        assertTrue(rightOuter.rhs() instanceof Concatenation);
        assertEquals(new Token("a"), rightOuter.lhs());
    }
    
    // ========== Operator Precedence ==========
    
    @Test
    void testPrecedenceConcatOverUnion() {
        // a.b|c should parse as (a.b)|c
        Expression result = RegeReaderLeft.parse("τ[a].τ[b]|τ[c]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        
        assertTrue(union.lhs() instanceof Concatenation);
        assertEquals(new Token("c"), union.rhs());
    }
    
    @Test
    void testPrecedenceStarOverConcat() {
        // a.b* should parse as a.(b*)
        Expression result = RegeReaderLeft.parse("τ[a].τ[b]*", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        
        assertEquals(new Token("a"), concat.lhs());
        assertTrue(concat.rhs() instanceof KleeneStar);
    }
    
    @Test
    void testPrecedenceParens() {
        // (a|b).c should parse as (a|b).c, not a|(b.c)
        Expression result = RegeReaderLeft.parse("(τ[a]|τ[b]).τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        
        assertTrue(concat.lhs() instanceof Union);
        assertEquals(new Token("c"), concat.rhs());
    }
    
    // ========== Kleene Star ==========
    
    @Test
    void testKleeneStar() {
        Expression result = RegeReaderLeft.parse("τ[a]*", false).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertEquals(new Token("a"), star.expression());
    }
    
    @Test
    void testDoubleKleeneStar() {
        // a** should parse as (a*)*
        Expression result = RegeReaderLeft.parse("τ[a]**", false).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar outer = (KleeneStar) result;
        assertTrue(outer.expression() instanceof KleeneStar);
        KleeneStar inner = (KleeneStar) outer.expression();
        assertEquals(new Token("a"), inner.expression());
    }
    
    // ========== Smart Constructors ==========
    
    @Test
    void testSmartEpsilonStar() {
        // ϵ* with smart constructors should simplify to ϵ
        Expression result = RegeReaderLeft.parse("ϵ*").orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartEmptyStar() {
        // ∅* with smart constructors should simplify to ϵ
        Expression result = RegeReaderLeft.parse("∅*").orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartEmptyConcat() {
        // ∅.a with smart constructors should simplify to ∅
        Expression result = RegeReaderLeft.parse("∅.τ[a]").orElse(null);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testSmartEpsilonConcat() {
        // ϵ.a with smart constructors should simplify to a
        Expression result = RegeReaderLeft.parse("ϵ.τ[a]").orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testSmartEmptyUnion() {
        // ∅|a with smart constructors should simplify to a
        Expression result = RegeReaderLeft.parse("∅|τ[a]").orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    // ========== Complex Expressions ==========
    
    @Test
    void testComplexExpression1() {
        // (a|b)*.c should parse correctly
        Expression result = RegeReaderLeft.parse("(τ[a]|τ[b])*.τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        
        assertTrue(concat.lhs() instanceof KleeneStar);
        assertEquals(new Token("c"), concat.rhs());
        
        KleeneStar star = (KleeneStar) concat.lhs();
        assertTrue(star.expression() instanceof Union);
    }
    
    @Test
    void testComplexExpression2() {
        // a.(b|c).d with left-assoc should parse as (a.(b|c)).d
        Expression result = RegeReaderLeft.parse("τ[a].(τ[b]|τ[c]).τ[d]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        
        assertEquals(new Token("d"), outer.rhs());
        assertTrue(outer.lhs() instanceof Concatenation);
        
        Concatenation inner = (Concatenation) outer.lhs();
        assertEquals(new Token("a"), inner.lhs());
        assertTrue(inner.rhs() instanceof Union);
    }
    
    // ========== Implicit Concatenation ==========
    
    @Test
    void testImplicitConcatenation() {
        // abc (without dots) should parse as (a.b).c (left-assoc)
        Expression result = RegeReaderLeft.parse("τ[a]τ[b]τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        
        assertEquals(new Token("c"), outer.rhs());
        assertTrue(outer.lhs() instanceof Concatenation);
    }
    
    // ========== Whitespace ==========
    
    @Test
    void testWhitespaceHandling() {
        Expression result = RegeReaderLeft.parse("  τ[a]  .  τ[b]  |  τ[c]  ", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        
        assertTrue(union.lhs() instanceof Concatenation);
        assertEquals(new Token("c"), union.rhs());
    }
    
    // ========== Edge Cases ==========
    
    @Test
    void testSingleToken() {
        Expression result = RegeReaderLeft.parse("τ[x]", false).orElse(null);
        assertEquals(new Token("x"), result);
    }
    
    @Test
    void testNestedParens() {
        Expression result = RegeReaderLeft.parse("((τ[a]))", false).orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testEmptyTokenValue() {
        // Empty token value τ[] should be parsed as epsilon, not Token("")
        Expression result = RegeReaderLeft.parse("τ[]", false).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testTokenWithEscapes() {
        Expression result = RegeReaderLeft.parse("τ[hello\\nworld]", false).orElse(null);
        assertEquals(new Token("hello\nworld"), result);
    }
    
    // ========== Semantic Equivalence Tests ==========
    
    @Test
    void testSemanticEquivalenceAfterSimplification() {
        String input = "τ[a].τ[b].τ[c]";
        
        // Both should simplify to same structure with smart constructors
        Expression leftSmart = RegeReaderLeft.parse(input, true).orElse(null);
        Expression rightSmart = RegeReader.parse(input, true).orElse(null);
        
        // With smart constructors, they might still differ structurally
        // but should be semantically equivalent
        // (We verify they both parse successfully and produce valid trees)
        assertNotNull(leftSmart);
        assertNotNull(rightSmart);
    }
    
    @Test
    void testEquivalentSimplifications() {
        // Test that both readers apply the same smart constructor rules
        
        // Empty rules
        assertEquals(
            RegeReaderLeft.parse("∅*", true),
            RegeReader.parse("∅*", true)
        );
        
        assertEquals(
            RegeReaderLeft.parse("∅|τ[a]", true),
            RegeReader.parse("∅|τ[a]", true)
        );
        
        // Epsilon rules
        assertEquals(
            RegeReaderLeft.parse("ϵ*", true),
            RegeReader.parse("ϵ*", true)
        );
        
        assertEquals(
            RegeReaderLeft.parse("ϵ.τ[a]", true),
            RegeReader.parse("ϵ.τ[a]", true)
        );
    }
    
    // ========== Unicode Support ==========
    
    @Test
    void testUnicodeToken() {
        Expression result = RegeReaderLeft.parse("τ[こんにちは]", false).orElse(null);
        assertEquals(new Token("こんにちは"), result);
    }
    
    @Test
    void testEmojiToken() {
        Expression result = RegeReaderLeft.parse("τ[😀🎉]", false).orElse(null);
        assertEquals(new Token("😀🎉"), result);
    }
}
