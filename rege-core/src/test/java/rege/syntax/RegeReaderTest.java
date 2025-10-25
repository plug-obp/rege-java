package rege.syntax;
import rege.reader.infra.*;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for RegeReader.
 */
class RegeReaderTest {
    
    // Basic Terminals
    
    @Test
    void testReadEmpty() {
        Expression result = RegeReader.parse("∅").orElse(null);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testReadEpsilon() {
        Expression result = RegeReader.parse("ϵ").orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadTokenWithTau() {
        Expression result = RegeReader.parse("τ[a]").orElse(null);
        assertEquals(new Token("a"), result);
        
        result = RegeReader.parse("τ[b]").orElse(null);
        assertEquals(new Token("b"), result);
    }
    
    @Test
    void testReadTokenWithT() {
        Expression result = RegeReader.parse("t[a]").orElse(null);
        assertEquals(new Token("a"), result);
        
        result = RegeReader.parse("t[b]").orElse(null);
        assertEquals(new Token("b"), result);
    }
    
    @Test
    void testReadTokenWithEscape() {
        // Escape sequences are processed: \] becomes ]
        Expression result = RegeReader.parse("τ[abc\\]d]").orElse(null);
        assertEquals(new Token("abc]d"), result);
    }
    
    @Test
    void testReadTokenEscapeNewline() {
        Expression result = RegeReader.parse("τ[hello\\nworld]").orElse(null);
        assertEquals(new Token("hello\nworld"), result);
    }
    
    @Test
    void testReadTokenEscapeTab() {
        Expression result = RegeReader.parse("τ[a\\tb]").orElse(null);
        assertEquals(new Token("a\tb"), result);
    }
    
    @Test
    void testReadTokenEscapeBackslash() {
        Expression result = RegeReader.parse("τ[a\\\\b]").orElse(null);
        assertEquals(new Token("a\\b"), result);
    }
    
    @Test
    void testReadTokenEscapeOpenBracket() {
        Expression result = RegeReader.parse("τ[a\\[b]").orElse(null);
        assertEquals(new Token("a[b"), result);
    }
    
    @Test
    void testReadTokenMultipleEscapes() {
        Expression result = RegeReader.parse("τ[\\n\\t\\]\\\\]").orElse(null);
        assertEquals(new Token("\n\t]\\"), result);
    }
    
    @Test
    void testReadTokenLongValue() {
        Expression result = RegeReader.parse("τ[hello world]").orElse(null);
        assertEquals(new Token("hello world"), result);
    }
    
    @Test
    void testReadTokenEmpty() {
        // Empty token value τ[] should be parsed as epsilon, not Token("")
        Expression result = RegeReader.parse("τ[]").orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    // Whitespace Handling
    
    @Test
    void testIgnoreSpacesAroundEmpty() {
        assertEquals(Expression.EMPTY, RegeReader.parse(" ∅").orElse(null));
        assertEquals(Expression.EMPTY, RegeReader.parse("∅ ").orElse(null));
        assertEquals(Expression.EMPTY, RegeReader.parse(" ∅ ").orElse(null));
    }
    
    @Test
    void testIgnoreTabsAroundEmpty() {
        assertEquals(Expression.EMPTY, RegeReader.parse("\t∅").orElse(null));
        assertEquals(Expression.EMPTY, RegeReader.parse("∅\t").orElse(null));
        assertEquals(Expression.EMPTY, RegeReader.parse("\t∅\t").orElse(null));
    }
    
    @Test
    void testIgnoreSpacesInConcatenation() {
        Expression result = RegeReader.parse("∅   ⋅τ[a]", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
        
        result = RegeReader.parse("∅\t⋅τ[a]", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
        
        result = RegeReader.parse("∅\t⋅    τ[a]", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
    }
    
    @Test
    void testSpaceInToken() {
        Expression result = RegeReader.parse("τ[a] | τ [b]").orElse(null);
        assertEquals(new Union(new Token("a"), new Token("b")), result);
    }
    
    // Concatenation
    
    @Test
    void testReadConcatenationImplicit() {
        Expression result = RegeReader.parse("∅ϵ", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadConcatenationDot() {
        Expression result = RegeReader.parse("∅.ϵ", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadConcatenationCdot() {
        Expression result = RegeReader.parse("∅⋅ϵ", false).orElse(null);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadMultipleConcatenations() {
        Expression result = RegeReader.parse("τ[a].τ[b].τ[c]", false).orElse(null);
        // Should parse as τ[a]⋅(τ[b]⋅τ[c]) (right-associative)
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    // Union
    
    @Test
    void testReadUnionPipe() {
        Expression result = RegeReader.parse("∅|ϵ", false).orElse(null);
        assertEquals(new Union(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadUnionCup() {
        Expression result = RegeReader.parse("∅ ∪ ϵ", false).orElse(null);
        assertEquals(new Union(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadMultipleUnions() {
        Expression result = RegeReader.parse("τ[a]|τ[b]|τ[c]", false).orElse(null);
        // Should parse as τ[a]|(τ[b]|τ[c]) (right-associative)
        assertTrue(result instanceof Union);
        Union outer = (Union) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Union);
    }
    
    // Kleene Star
    
    @Test
    void testReadKleeneStar() {
        // ϵ* with smart constructors should simplify to ϵ
        Expression result = RegeReader.parse("ϵ*").orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadDoubleKleeneStar() {
        Expression result = RegeReader.parse("τ[a]**", false).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar outer = (KleeneStar) result;
        assertTrue(outer.expression() instanceof KleeneStar);
        KleeneStar inner = (KleeneStar) outer.expression();
        assertEquals(new Token("a"), inner.expression());
    }
    
    // Parentheses
    
    @Test
    void testReadParensSimple() {
        Expression result = RegeReader.parse("(ϵ)", false).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadParensWithUnion() {
        // ϵ(ϵ|ϵ) is implicit concatenation: ϵ⋅(ϵ|ϵ) right-associative
        Expression result = RegeReader.parse("ϵ(ϵ|ϵ)", false).orElse(null);
        assertEquals(
            new Concatenation(Expression.EPSILON, new Union(Expression.EPSILON, Expression.EPSILON)),
            result
        );
    }
    
    @Test
    void testReadParensPrecedence() {
        // ϵϵ|ϵ parses as (ϵ⋅ϵ)|ϵ due to standard precedence (concat > union)
        Expression result1 = RegeReader.parse("ϵϵ|ϵ", false).orElse(null);
        assertEquals(
            new Union(new Concatenation(Expression.EPSILON, Expression.EPSILON), Expression.EPSILON),
            result1
        );
        
        // (ϵϵ)|ϵ explicitly groups the concatenation (same result)
        Expression result2 = RegeReader.parse("(ϵϵ)|ϵ", false).orElse(null);
        assertEquals(
            new Union(new Concatenation(Expression.EPSILON, Expression.EPSILON), Expression.EPSILON),
            result2
        );
    }
    
    @Test
    void testReadNestedParens() {
        Expression result = RegeReader.parse("((τ[a]))", false).orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testReadComplexParens() {
        Expression result = RegeReader.parse("(τ[a]|τ[b])⋅(τ[c]|τ[d])", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof Union);
        assertTrue(concat.rhs() instanceof Union);
    }
    
    // Smart Constructors (isSmart = true)
    
    @Test
    void testSmartUnionIdempotent() {
        Expression result = RegeReader.parse("ϵ|ϵ", true).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionIdentityLeft() {
        Expression result = RegeReader.parse("ϵ|∅", true).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionIdentityRight() {
        Expression result = RegeReader.parse("∅|ϵ", true).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionTokens() {
        Expression result = RegeReader.parse("τ[a]|τ[a]", true).orElse(null);
        assertEquals(new Token("a"), result);
        
        result = RegeReader.parse("τ[a]|τ[b]", true).orElse(null);
        assertTrue(result instanceof Union);
    }
    
    @Test
    void testSmartConcatenationAnnihilatorLeft() {
        Expression result = RegeReader.parse("∅τ[a]", true).orElse(null);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testSmartConcatenationAnnihilatorRight() {
        Expression result = RegeReader.parse("τ[a]∅", true).orElse(null);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testSmartConcatenationIdentityLeft() {
        Expression result = RegeReader.parse("ϵτ[a]", true).orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testSmartConcatenationIdentityRight() {
        Expression result = RegeReader.parse("τ[a]ϵ", true).orElse(null);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testSmartKleeneStar() {
        Expression result = RegeReader.parse("τ[a]**", true).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertEquals(new Token("a"), star.expression());
    }
    
    @Test
    void testSmartKleeneStarEmpty() {
        Expression result = RegeReader.parse("∅*", true).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartKleeneStarEpsilon() {
        Expression result = RegeReader.parse("ϵ*", true).orElse(null);
        assertEquals(Expression.EPSILON, result);
    }
    
    // Complex Expressions
    
    @Test
    void testComplexExpression1() {
        // (a|b)*
        Expression result = RegeReader.parse("(τ[a]|τ[b])*", false).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertTrue(star.expression() instanceof Union);
    }
    
    @Test
    void testComplexExpression2() {
        // a⋅b|c⋅d should parse as (a⋅b)|(c⋅d) due to standard precedence
        Expression result = RegeReader.parse("τ[a]⋅τ[b]|τ[c]⋅τ[d]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        assertTrue(union.lhs() instanceof Concatenation);
        assertTrue(union.rhs() instanceof Concatenation);
    }
    
    @Test
    void testComplexExpression3() {
        // (a|b)⋅(c|d)*
        Expression result = RegeReader.parse("(τ[a]|τ[b])⋅(τ[c]|τ[d])*", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof Union);
        assertTrue(concat.rhs() instanceof KleeneStar);
    }
    
    @Test
    void testComplexExpression4() {
        // a*b*c*
        Expression result = RegeReader.parse("τ[a]*τ[b]*τ[c]*", false).orElse(null);
        assertTrue(result instanceof Concatenation);
    }
    
    @Test
    void testComplexExpressionSmart() {
        // (ϵ|∅)* with smart constructors should simplify
        Expression result = RegeReader.parse("(ϵ|∅)*", true).orElse(null);
        assertEquals(Expression.EPSILON, result); // (ϵ|∅) = ϵ, ϵ* = ϵ
    }
    
    // Edge Cases
    
    @Test
    void testEmptyString() {
        Expression result = RegeReader.parse("").orElse(null);
        assertNull(result);
    }
    
    @Test
    void testOnlyWhitespace() {
        Expression result = RegeReader.parse("   ").orElse(null);
        assertNull(result);
    }
    
    @Test
    void testInvalidToken() {
        Expression result = RegeReader.parse("τ[abc").orElse(null);
        assertNull(result);
    }
    
    @Test
    void testInvalidParens() {
        Expression result = RegeReader.parse("(τ[a]").orElse(null);
        assertNull(result);
    }
    
    @Test
    void testUnexpectedCharacter() {
        Expression result = RegeReader.parse("@").orElse(null);
        assertNull(result);
    }
    
    // Real-world Examples
    
    @Test
    void testEmailLikePattern() {
        // username@domain
        Expression result = RegeReader.parse("τ[user]⋅τ[@]⋅τ[domain]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
    }
    
    @Test
    void testOptionalPattern() {
        // a(b|ϵ)c means "abc" or "ac" - parses as a⋅((b|ϵ)⋅c) (right-associative)
        Expression result = RegeReader.parse("τ[a]⋅(τ[b]|ϵ)⋅τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    @Test
    void testRepetitionPattern() {
        // (ab)* - zero or more repetitions of "ab"
        Expression result = RegeReader.parse("(τ[a]⋅τ[b])*", false).orElse(null);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertTrue(star.expression() instanceof Concatenation);
    }
    
    @Test
    void testAlternativesPattern() {
        // (yes|no|maybe)
        Expression result = RegeReader.parse("(τ[yes]|τ[no]|τ[maybe])", false).orElse(null);
        assertTrue(result instanceof Union);
    }
    
    // Operator Precedence Tests (Standard regex: star > concat > union)
    
    @Test
    void testPrecedenceStarHighest() {
        // a*b should parse as (a*)⋅b, not (a⋅b)*
        Expression result = RegeReader.parse("τ[a]*τ[b]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof KleeneStar);
        assertEquals(new Token("b"), concat.rhs());
    }
    
    @Test
    void testPrecedenceConcatHigherThanUnion() {
        // a⋅b|c should parse as (a⋅b)|c, not a⋅(b|c)
        Expression result = RegeReader.parse("τ[a]τ[b]|τ[c]", false).orElse(null);
        // With correct precedence: concat binds tighter than union
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        assertTrue(union.lhs() instanceof Concatenation);
        assertEquals(new Token("c"), union.rhs());
    }
    
    @Test
    void testLeftAssociativityUnion() {
        // a|b|c should parse as a|(b|c) (right-associative)
        Expression result = RegeReader.parse("τ[a]|τ[b]|τ[c]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union outer = (Union) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Union);
    }
    
    @Test
    void testLeftAssociativityConcat() {
        // abc should parse as a⋅(b⋅c) (right-associative)
        Expression result = RegeReader.parse("τ[a]τ[b]τ[c]", false).orElse(null);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    // Unicode and Special Characters
    
    @Test
    void testUnicodeInToken() {
        Expression result = RegeReader.parse("τ[こんにちは]").orElse(null);
        assertEquals(new Token("こんにちは"), result);
    }
    
    @Test
    void testEmojiInToken() {
        Expression result = RegeReader.parse("τ[😀🎉]").orElse(null);
        assertEquals(new Token("😀🎉"), result);
    }
    
    @Test
    void testSpecialCharsInToken() {
        Expression result = RegeReader.parse("τ[!@#$%^&*()]").orElse(null);
        assertEquals(new Token("!@#$%^&*()"), result);
    }
    
    // Additional Precedence Tests (Standard regex: star > concat > union)
    
    @Test
    void testPrecedenceUnionLowest() {
        // a|b.c should parse as a|(b.c)
        Expression result = RegeReader.parse("τ[a]|τ[b].τ[c]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        assertEquals(new Token("a"), union.lhs());
        assertTrue(union.rhs() instanceof Concatenation);
    }
    
    @Test
    void testPrecedenceStarVsUnion() {
        // a*|b should parse as (a*)|b
        Expression result = RegeReader.parse("τ[a]*|τ[b]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        assertTrue(union.lhs() instanceof KleeneStar);
        assertEquals(new Token("b"), union.rhs());
    }
    
    @Test
    void testPrecedenceComplexMultiOp() {
        // a*b|c*d should parse as ((a*).b)|((c*).d)
        Expression result = RegeReader.parse("τ[a]*τ[b]|τ[c]*τ[d]", false).orElse(null);
        assertTrue(result instanceof Union);
        Union union = (Union) result;
        
        // Left side: (a*).b
        assertTrue(union.lhs() instanceof Concatenation);
        Concatenation leftConcat = (Concatenation) union.lhs();
        assertTrue(leftConcat.lhs() instanceof KleeneStar);
        assertEquals(new Token("b"), leftConcat.rhs());
        
        // Right side: (c*).d  
        assertTrue(union.rhs() instanceof Concatenation);
        Concatenation rightConcat = (Concatenation) union.rhs();
        assertTrue(rightConcat.lhs() instanceof KleeneStar);
        assertEquals(new Token("d"), rightConcat.rhs());
    }
}
