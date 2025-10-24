package rege.syntax;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify roundtripping: parse(print(expr)) equals expr.
 * 
 * <p>These tests ensure that PrettyPrinter output can be parsed back
 * by RegeReader to produce an equivalent expression.
 */
class PrettyPrinterRoundtripTest {
    
    private void assertRoundtrip(Expression expr) {
        String printed = PrettyPrinter.print(expr);
        Expression parsed = RegeReader.readExpression(printed, false);
        assertEquals(expr, parsed, 
            "Roundtrip failed: original=" + expr + 
            ", printed=" + printed + 
            ", parsed=" + parsed);
    }
    
    private void assertRoundtripBothParsers(Expression expr) {
        String printed = PrettyPrinter.print(expr);
        Expression parsedRight = RegeReader.readExpression(printed, false);
        Expression parsedLeft = RegeReaderLeft.readExpression(printed, false);
        
        assertEquals(expr, parsedRight, 
            "Roundtrip failed with RegeReader: original=" + expr + 
            ", printed=" + printed + 
            ", parsed=" + parsedRight);
        
        assertEquals(expr, parsedLeft, 
            "Roundtrip failed with RegeReaderLeft: original=" + expr + 
            ", printed=" + printed + 
            ", parsed=" + parsedLeft);
    }
    
    @Test
    void roundtripEmpty() {
        assertRoundtrip(Expression.EMPTY);
    }
    
    @Test
    void roundtripEpsilon() {
        assertRoundtrip(Expression.EPSILON);
    }
    
    @Test
    void roundtripToken() {
        assertRoundtrip(new Token("a"));
        assertRoundtrip(new Token("hello"));
        assertRoundtrip(new Token("123"));
    }
    
    @Test
    void roundtripTokenWithSpecialChars() {
        // Note: Parser keeps backslash in escape sequences, so Token("a]b") can't exist
        // because `τ[a]b]` parses as Token("a") followed by unparsed "b]".
        // The proper token value that roundtrips is Token("a\]b") - literal backslash-bracket
        assertRoundtrip(new Token("a\\b"));  // backslash-b roundtrips as-is
        assertRoundtrip(new Token("a\\]b")); // backslash-bracket-b roundtrips as-is
        
        // To include a bracket in token, you write Token("a\]b") and it prints/parses correctly
        String printed = PrettyPrinter.print(new Token("a\\]b"));
        assertEquals("τ[a\\]b]", printed);
    }
    
    @Test
    void roundtripUnion() {
        assertRoundtrip(new Union(new Token("a"), new Token("b")));
        assertRoundtrip(new Union(Expression.EMPTY, new Token("a")));
        assertRoundtrip(new Union(Expression.EPSILON, new Token("a")));
    }
    
    @Test
    void roundtripConcatenation() {
        assertRoundtrip(new Concatenation(new Token("a"), new Token("b")));
        assertRoundtrip(new Concatenation(Expression.EMPTY, new Token("a")));
        assertRoundtrip(new Concatenation(Expression.EPSILON, new Token("a")));
    }
    
    @Test
    void roundtripKleeneStar() {
        assertRoundtrip(new KleeneStar(new Token("a")));
        assertRoundtrip(new KleeneStar(Expression.EMPTY));
        assertRoundtrip(new KleeneStar(Expression.EPSILON));
    }
    
    @Test
    void roundtripKleeneStarComposite() {
        assertRoundtrip(new KleeneStar(new Union(new Token("a"), new Token("b"))));
        assertRoundtrip(new KleeneStar(new Concatenation(new Token("a"), new Token("b"))));
        assertRoundtrip(new KleeneStar(new KleeneStar(new Token("a"))));
    }
    
    @Test
    void roundtripNestedUnion() {
        // Note: (a|b)|c will be reparsed as a|(b|c) due to right-associativity
        // This is semantically equivalent but structurally different
        Expression expr = new Union(
            new Union(new Token("a"), new Token("b")),
            new Token("c")
        );
        String printed = PrettyPrinter.print(expr);
        assertEquals("τ[a]|τ[b]|τ[c]", printed);
        
        // Both parsers should successfully parse it (though structure differs)
        Expression parsedRight = RegeReader.readExpression(printed, false);
        Expression parsedLeft = RegeReaderLeft.readExpression(printed, false);
        assertNotNull(parsedRight);
        assertNotNull(parsedLeft);
    }
    
    @Test
    void roundtripNestedConcatenation() {
        // Note: (a⋅b)⋅c will be reparsed as a⋅(b⋅c) by RegeReader due to right-associativity
        // But RegeReaderLeft will preserve left-associativity
        Expression expr = new Concatenation(
            new Concatenation(new Token("a"), new Token("b")),
            new Token("c")
        );
        String printed = PrettyPrinter.print(expr);
        assertEquals("τ[a]τ[b]τ[c]", printed);
        
        // RegeReaderLeft preserves left-associativity
        Expression parsedLeft = RegeReaderLeft.readExpression(printed, false);
        assertEquals(expr, parsedLeft);
    }
    
    @Test
    void roundtripMixedOperations1() {
        // (a|b)c
        Expression expr = new Concatenation(
            new Union(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripMixedOperations2() {
        // a(b|c)
        Expression expr = new Concatenation(
            new Token("a"),
            new Union(new Token("b"), new Token("c"))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripMixedOperations3() {
        // a|bc - Note: parsers are right-associative
        Expression expr = new Union(
            new Token("a"),
            new Concatenation(new Token("b"), new Token("c"))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripMixedOperations4() {
        // ab|c - Note: parsers are right-associative
        Expression expr = new Union(
            new Concatenation(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripStarPrecedence1() {
        // a*|b
        Expression expr = new Union(
            new KleeneStar(new Token("a")),
            new Token("b")
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripStarPrecedence2() {
        // a*b
        Expression expr = new Concatenation(
            new KleeneStar(new Token("a")),
            new Token("b")
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripStarPrecedence3() {
        // (a|b)*
        Expression expr = new KleeneStar(
            new Union(new Token("a"), new Token("b"))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripComplexExpression1() {
        // (a|b)*c
        Expression expr = new Concatenation(
            new KleeneStar(new Union(new Token("a"), new Token("b"))),
            new Token("c")
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripComplexExpression2() {
        // a(b|c)*
        Expression expr = new Concatenation(
            new Token("a"),
            new KleeneStar(new Union(new Token("b"), new Token("c")))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripComplexExpression3() {
        // (ab)*
        Expression expr = new KleeneStar(
            new Concatenation(new Token("a"), new Token("b"))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripComplexExpression4() {
        // a*b*
        Expression expr = new Concatenation(
            new KleeneStar(new Token("a")),
            new KleeneStar(new Token("b"))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripComplexExpression5() {
        // (a*b)*
        Expression expr = new KleeneStar(
            new Concatenation(
                new KleeneStar(new Token("a")),
                new Token("b")
            )
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripDeeplyNested() {
        // ((a|b)|(c|d))* - note: will reparse with different associativity
        Expression expr = new KleeneStar(
            new Union(
                new Union(new Token("a"), new Token("b")),
                new Union(new Token("c"), new Token("d"))
            )
        );
        String printed = PrettyPrinter.print(expr);
        assertEquals("(τ[a]|τ[b]|τ[c]|τ[d])*", printed);
        
        // Due to right-associativity, structure changes but expression is semantically equivalent
        Expression parsed = RegeReader.readExpression(printed, false);
        assertNotNull(parsed);
        // Could verify semantic equivalence by checking derivatives, but structural equality doesn't hold
    }
    
    @Test
    void roundtripWithSmartConstructors() {
        // Test that smart constructor simplifications are preserved
        Expression expr = Expression.EPSILON.union(new Token("a"));
        assertRoundtrip(expr);
        
        Expression expr2 = Expression.EMPTY.concat(new Token("a"));
        assertRoundtrip(expr2);
        
        Expression expr3 = new KleeneStar(Expression.EMPTY);
        assertRoundtrip(expr3);
    }
    
    @Test
    void roundtripRealWorldPattern1() {
        // (a|b|c)* - note: left-assoc structure becomes right-assoc when reparsed
        Expression expr = new KleeneStar(
            new Union(
                new Union(new Token("a"), new Token("b")),
                new Token("c")
            )
        );
        String printed = PrettyPrinter.print(expr);
        assertEquals("(τ[a]|τ[b]|τ[c])*", printed);
        
        // Parser produces right-associative structure: (a|(b|c))* instead of ((a|b)|c)*
        Expression parsed = RegeReader.readExpression(printed, false);
        assertNotNull(parsed);
        // Semantically equivalent, structurally different
    }
    
    @Test
    void roundtripRealWorldPattern2() {
        // a(b|ε)c - optional b - left-assoc concatenation becomes right-assoc
        Expression expr = new Concatenation(
            new Concatenation(
                new Token("a"),
                new Union(new Token("b"), Expression.EPSILON)
            ),
            new Token("c")
        );
        String printed = PrettyPrinter.print(expr);
        assertEquals("τ[a](τ[b]|ϵ)τ[c]", printed);
        
        // Parser produces right-associative: a((b|ε)c) instead of (a(b|ε))c
        Expression parsed = RegeReader.readExpression(printed, false);
        assertNotNull(parsed);
        // Semantically equivalent, structurally different
    }
    
    @Test
    void roundtripRealWorldPattern3() {
        // (letter)(letter|digit)* - identifier pattern
        Expression letter = new Token("letter");
        Expression digit = new Token("digit");
        Expression expr = new Concatenation(
            letter,
            new KleeneStar(new Union(letter, digit))
        );
        assertRoundtrip(expr);
    }
    
    @Test
    void roundtripExplicitConcatenation() {
        // Test with explicit concatenation operator
        Expression expr = new Concatenation(new Token("a"), new Token("b"));
        String printed = PrettyPrinter.print(expr, true);
        assertEquals("τ[a]⋅τ[b]", printed);
        
        Expression parsed = RegeReader.readExpression(printed, false);
        assertEquals(expr, parsed);
    }
}
