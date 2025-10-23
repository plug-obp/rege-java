package rege.syntax;

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
        Expression result = RegeReader.readExpression("∅");
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testReadEpsilon() {
        Expression result = RegeReader.readExpression("ϵ");
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadTokenWithTau() {
        Expression result = RegeReader.readExpression("τ[a]");
        assertEquals(new Token("a"), result);
        
        result = RegeReader.readExpression("τ[b]");
        assertEquals(new Token("b"), result);
    }
    
    @Test
    void testReadTokenWithT() {
        Expression result = RegeReader.readExpression("t[a]");
        assertEquals(new Token("a"), result);
        
        result = RegeReader.readExpression("t[b]");
        assertEquals(new Token("b"), result);
    }
    
    @Test
    void testReadTokenWithEscape() {
        Expression result = RegeReader.readExpression("τ[abc\\]d]");
        assertEquals(new Token("abc\\]d"), result);
    }
    
    @Test
    void testReadTokenLongValue() {
        Expression result = RegeReader.readExpression("τ[hello world]");
        assertEquals(new Token("hello world"), result);
    }
    
    @Test
    void testReadTokenEmpty() {
        Expression result = RegeReader.readExpression("τ[]");
        assertEquals(new Token(""), result);
    }
    
    // Whitespace Handling
    
    @Test
    void testIgnoreSpacesAroundEmpty() {
        assertEquals(Expression.EMPTY, RegeReader.readExpression(" ∅"));
        assertEquals(Expression.EMPTY, RegeReader.readExpression("∅ "));
        assertEquals(Expression.EMPTY, RegeReader.readExpression(" ∅ "));
    }
    
    @Test
    void testIgnoreTabsAroundEmpty() {
        assertEquals(Expression.EMPTY, RegeReader.readExpression("\t∅"));
        assertEquals(Expression.EMPTY, RegeReader.readExpression("∅\t"));
        assertEquals(Expression.EMPTY, RegeReader.readExpression("\t∅\t"));
    }
    
    @Test
    void testIgnoreSpacesInConcatenation() {
        Expression result = RegeReader.readExpression("∅   ⋅τ[a]", false);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
        
        result = RegeReader.readExpression("∅\t⋅τ[a]", false);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
        
        result = RegeReader.readExpression("∅\t⋅    τ[a]", false);
        assertEquals(new Concatenation(Expression.EMPTY, new Token("a")), result);
    }
    
    @Test
    void testSpaceInToken() {
        Expression result = RegeReader.readExpression("τ[a] | τ [b]");
        assertEquals(new Union(new Token("a"), new Token("b")), result);
    }
    
    // Concatenation
    
    @Test
    void testReadConcatenationImplicit() {
        Expression result = RegeReader.readExpression("∅ϵ", false);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadConcatenationDot() {
        Expression result = RegeReader.readExpression("∅.ϵ", false);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadConcatenationCdot() {
        Expression result = RegeReader.readExpression("∅⋅ϵ", false);
        assertEquals(new Concatenation(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadMultipleConcatenations() {
        Expression result = RegeReader.readExpression("τ[a].τ[b].τ[c]", false);
        // Should parse as τ[a]⋅(τ[b]⋅τ[c]) (right-associative)
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    // Union
    
    @Test
    void testReadUnionPipe() {
        Expression result = RegeReader.readExpression("∅|ϵ", false);
        assertEquals(new Union(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadUnionCup() {
        Expression result = RegeReader.readExpression("∅ ∪ ϵ", false);
        assertEquals(new Union(Expression.EMPTY, Expression.EPSILON), result);
    }
    
    @Test
    void testReadMultipleUnions() {
        Expression result = RegeReader.readExpression("τ[a]|τ[b]|τ[c]", false);
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
        Expression result = RegeReader.readExpression("ϵ*");
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadDoubleKleeneStar() {
        Expression result = RegeReader.readExpression("τ[a]**", false);
        assertTrue(result instanceof KleeneStar);
        KleeneStar outer = (KleeneStar) result;
        assertTrue(outer.expression() instanceof KleeneStar);
        KleeneStar inner = (KleeneStar) outer.expression();
        assertEquals(new Token("a"), inner.expression());
    }
    
    // Parentheses
    
    @Test
    void testReadParensSimple() {
        Expression result = RegeReader.readExpression("(ϵ)", false);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testReadParensWithUnion() {
        Expression result = RegeReader.readExpression("ϵ(ϵ|ϵ)", false);
        assertEquals(
            new Concatenation(Expression.EPSILON, new Union(Expression.EPSILON, Expression.EPSILON)),
            result
        );
    }
    
    @Test
    void testReadParensPrecedence() {
        // ϵϵ|ϵ parses as (ϵ⋅ϵ)|ϵ
        Expression result1 = RegeReader.readExpression("ϵϵ|ϵ", false);
        assertEquals(
            new Concatenation(Expression.EPSILON, new Union(Expression.EPSILON, Expression.EPSILON)),
            result1
        );
        
        // (ϵϵ)|ϵ explicitly groups the concatenation
        Expression result2 = RegeReader.readExpression("(ϵϵ)|ϵ", false);
        assertEquals(
            new Union(new Concatenation(Expression.EPSILON, Expression.EPSILON), Expression.EPSILON),
            result2
        );
    }
    
    @Test
    void testReadNestedParens() {
        Expression result = RegeReader.readExpression("((τ[a]))", false);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testReadComplexParens() {
        Expression result = RegeReader.readExpression("(τ[a]|τ[b])⋅(τ[c]|τ[d])", false);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof Union);
        assertTrue(concat.rhs() instanceof Union);
    }
    
    // Smart Constructors (isSmart = true)
    
    @Test
    void testSmartUnionIdempotent() {
        Expression result = RegeReader.readExpression("ϵ|ϵ", true);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionIdentityLeft() {
        Expression result = RegeReader.readExpression("ϵ|∅", true);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionIdentityRight() {
        Expression result = RegeReader.readExpression("∅|ϵ", true);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartUnionTokens() {
        Expression result = RegeReader.readExpression("τ[a]|τ[a]", true);
        assertEquals(new Token("a"), result);
        
        result = RegeReader.readExpression("τ[a]|τ[b]", true);
        assertTrue(result instanceof Union);
    }
    
    @Test
    void testSmartConcatenationAnnihilatorLeft() {
        Expression result = RegeReader.readExpression("∅τ[a]", true);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testSmartConcatenationAnnihilatorRight() {
        Expression result = RegeReader.readExpression("τ[a]∅", true);
        assertEquals(Expression.EMPTY, result);
    }
    
    @Test
    void testSmartConcatenationIdentityLeft() {
        Expression result = RegeReader.readExpression("ϵτ[a]", true);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testSmartConcatenationIdentityRight() {
        Expression result = RegeReader.readExpression("τ[a]ϵ", true);
        assertEquals(new Token("a"), result);
    }
    
    @Test
    void testSmartKleeneStar() {
        Expression result = RegeReader.readExpression("τ[a]**", true);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertEquals(new Token("a"), star.expression());
    }
    
    @Test
    void testSmartKleeneStarEmpty() {
        Expression result = RegeReader.readExpression("∅*", true);
        assertEquals(Expression.EPSILON, result);
    }
    
    @Test
    void testSmartKleeneStarEpsilon() {
        Expression result = RegeReader.readExpression("ϵ*", true);
        assertEquals(Expression.EPSILON, result);
    }
    
    // Complex Expressions
    
    @Test
    void testComplexExpression1() {
        // (a|b)*
        Expression result = RegeReader.readExpression("(τ[a]|τ[b])*", false);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertTrue(star.expression() instanceof Union);
    }
    
    @Test
    void testComplexExpression2() {
        // a⋅b|c⋅d
        Expression result = RegeReader.readExpression("τ[a]⋅τ[b]|τ[c]⋅τ[d]", false);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.rhs() instanceof Union);
    }
    
    @Test
    void testComplexExpression3() {
        // (a|b)⋅(c|d)*
        Expression result = RegeReader.readExpression("(τ[a]|τ[b])⋅(τ[c]|τ[d])*", false);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof Union);
        assertTrue(concat.rhs() instanceof KleeneStar);
    }
    
    @Test
    void testComplexExpression4() {
        // a*b*c*
        Expression result = RegeReader.readExpression("τ[a]*τ[b]*τ[c]*", false);
        assertTrue(result instanceof Concatenation);
    }
    
    @Test
    void testComplexExpressionSmart() {
        // (ϵ|∅)* with smart constructors should simplify
        Expression result = RegeReader.readExpression("(ϵ|∅)*", true);
        assertEquals(Expression.EPSILON, result); // (ϵ|∅) = ϵ, ϵ* = ϵ
    }
    
    // Edge Cases
    
    @Test
    void testEmptyString() {
        Expression result = RegeReader.readExpression("");
        assertNull(result);
    }
    
    @Test
    void testOnlyWhitespace() {
        Expression result = RegeReader.readExpression("   ");
        assertNull(result);
    }
    
    @Test
    void testInvalidToken() {
        Expression result = RegeReader.readExpression("τ[abc");
        assertNull(result);
    }
    
    @Test
    void testInvalidParens() {
        Expression result = RegeReader.readExpression("(τ[a]");
        assertNull(result);
    }
    
    @Test
    void testUnexpectedCharacter() {
        Expression result = RegeReader.readExpression("@");
        assertNull(result);
    }
    
    // Real-world Examples
    
    @Test
    void testEmailLikePattern() {
        // username@domain
        Expression result = RegeReader.readExpression("τ[user]⋅τ[@]⋅τ[domain]", false);
        assertTrue(result instanceof Concatenation);
    }
    
    @Test
    void testOptionalPattern() {
        // a(b|ϵ)c means "abc" or "ac" - parses as a⋅((b|ϵ)⋅c) (right-associative)
        Expression result = RegeReader.readExpression("τ[a]⋅(τ[b]|ϵ)⋅τ[c]", false);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    @Test
    void testRepetitionPattern() {
        // (ab)* - zero or more repetitions of "ab"
        Expression result = RegeReader.readExpression("(τ[a]⋅τ[b])*", false);
        assertTrue(result instanceof KleeneStar);
        KleeneStar star = (KleeneStar) result;
        assertTrue(star.expression() instanceof Concatenation);
    }
    
    @Test
    void testAlternativesPattern() {
        // (yes|no|maybe)
        Expression result = RegeReader.readExpression("(τ[yes]|τ[no]|τ[maybe])", false);
        assertTrue(result instanceof Union);
    }
    
    // Operator Precedence Tests
    
    @Test
    void testPrecedenceStarOverConcat() {
        // a*b should parse as (a*)⋅b, not (a⋅b)*
        Expression result = RegeReader.readExpression("τ[a]*τ[b]", false);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.lhs() instanceof KleeneStar);
        assertEquals(new Token("b"), concat.rhs());
    }
    
    @Test
    void testPrecedenceConcatOverUnion() {
        // a⋅b|c should parse as (a⋅b)|c, not a⋅(b|c)
        Expression result = RegeReader.readExpression("τ[a]τ[b]|τ[c]", false);
        assertTrue(result instanceof Concatenation);
        Concatenation concat = (Concatenation) result;
        assertTrue(concat.rhs() instanceof Union);
    }
    
    @Test
    void testLeftAssociativityUnion() {
        // a|b|c should parse as a|(b|c) (right-associative)
        Expression result = RegeReader.readExpression("τ[a]|τ[b]|τ[c]", false);
        assertTrue(result instanceof Union);
        Union outer = (Union) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Union);
    }
    
    @Test
    void testLeftAssociativityConcat() {
        // abc should parse as a⋅(b⋅c) (right-associative)
        Expression result = RegeReader.readExpression("τ[a]τ[b]τ[c]", false);
        assertTrue(result instanceof Concatenation);
        Concatenation outer = (Concatenation) result;
        assertEquals(new Token("a"), outer.lhs());
        assertTrue(outer.rhs() instanceof Concatenation);
    }
    
    // Unicode and Special Characters
    
    @Test
    void testUnicodeInToken() {
        Expression result = RegeReader.readExpression("τ[こんにちは]");
        assertEquals(new Token("こんにちは"), result);
    }
    
    @Test
    void testEmojiInToken() {
        Expression result = RegeReader.readExpression("τ[😀🎉]");
        assertEquals(new Token("😀🎉"), result);
    }
    
    @Test
    void testSpecialCharsInToken() {
        Expression result = RegeReader.readExpression("τ[!@#$%^&*()]");
        assertEquals(new Token("!@#$%^&*()"), result);
    }
}
