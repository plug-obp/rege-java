package rege.syntax;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import static org.junit.jupiter.api.Assertions.*;

class PrettyPrinterTest {
    
    @Test
    void printEmpty() {
        assertEquals("∅", PrettyPrinter.print(Expression.EMPTY));
    }
    
    @Test
    void printEpsilon() {
        assertEquals("ϵ", PrettyPrinter.print(Expression.EPSILON));
    }
    
    @Test
    void printToken() {
        assertEquals("τ[a]", PrettyPrinter.print(new Token("a")));
        assertEquals("τ[hello]", PrettyPrinter.print(new Token("hello")));
        assertEquals("τ[123]", PrettyPrinter.print(new Token("123")));
    }
    
    @Test
    void printTokenWithEscaping() {
        // Backslash in token value gets escaped
        assertEquals("τ[a\\\\b]", PrettyPrinter.print(new Token("a\\b")));
        
        // Closing bracket should be escaped
        assertEquals("τ[a\\]b]", PrettyPrinter.print(new Token("a]b")));
        
        // Opening bracket should be escaped
        assertEquals("τ[a\\[b]", PrettyPrinter.print(new Token("a[b")));
        
        // Newline should be escaped
        assertEquals("τ[hello\\nworld]", PrettyPrinter.print(new Token("hello\nworld")));
        
        // Tab should be escaped
        assertEquals("τ[a\\tb]", PrettyPrinter.print(new Token("a\tb")));
        
        // Multiple special characters
        assertEquals("τ[\\n\\t\\]\\\\]", PrettyPrinter.print(new Token("\n\t]\\")));
    }
    
    @Test
    void printUnion() {
        Expression expr = new Union(new Token("a"), new Token("b"));
        assertEquals("τ[a]|τ[b]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printUnionWithEmpty() {
        Expression expr = new Union(Expression.EMPTY, new Token("a"));
        assertEquals("∅|τ[a]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printUnionWithEpsilon() {
        Expression expr = new Union(Expression.EPSILON, new Token("a"));
        assertEquals("ϵ|τ[a]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printConcatenation() {
        Expression expr = new Concatenation(new Token("a"), new Token("b"));
        assertEquals("τ[a]τ[b]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printConcatenationExplicit() {
        Expression expr = new Concatenation(new Token("a"), new Token("b"));
        assertEquals("τ[a]⋅τ[b]", PrettyPrinter.print(expr, true));
    }
    
    @Test
    void printConcatenationWithEmpty() {
        Expression expr = new Concatenation(Expression.EMPTY, new Token("a"));
        assertEquals("∅τ[a]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printConcatenationWithEpsilon() {
        Expression expr = new Concatenation(Expression.EPSILON, new Token("a"));
        assertEquals("ϵτ[a]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarToken() {
        Expression expr = new KleeneStar(new Token("a"));
        assertEquals("τ[a]*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarEmpty() {
        Expression expr = new KleeneStar(Expression.EMPTY);
        assertEquals("∅*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarEpsilon() {
        Expression expr = new KleeneStar(Expression.EPSILON);
        assertEquals("ϵ*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarUnion() {
        Expression expr = new KleeneStar(new Union(new Token("a"), new Token("b")));
        assertEquals("(τ[a]|τ[b])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarConcatenation() {
        Expression expr = new KleeneStar(new Concatenation(new Token("a"), new Token("b")));
        assertEquals("(τ[a]τ[b])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printKleeneStarNested() {
        Expression expr = new KleeneStar(new KleeneStar(new Token("a")));
        assertEquals("(τ[a]*)*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printUnionPrecedence() {
        // a|b|c should not add extra parentheses
        Expression expr = new Union(
            new Union(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertEquals("τ[a]|τ[b]|τ[c]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printConcatenationPrecedence() {
        // abc should not add extra parentheses
        Expression expr = new Concatenation(
            new Concatenation(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertEquals("τ[a]τ[b]τ[c]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence1() {
        // (a|b)c requires parentheses around union
        Expression expr = new Concatenation(
            new Union(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertEquals("(τ[a]|τ[b])τ[c]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence2() {
        // a(b|c) requires parentheses around union
        Expression expr = new Concatenation(
            new Token("a"),
            new Union(new Token("b"), new Token("c"))
        );
        assertEquals("τ[a](τ[b]|τ[c])", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence3() {
        // a|bc - concatenation on right doesn't need parens
        Expression expr = new Union(
            new Token("a"),
            new Concatenation(new Token("b"), new Token("c"))
        );
        assertEquals("τ[a]|(τ[b]τ[c])", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence4() {
        // ab|c - concatenation on left needs parens
        Expression expr = new Union(
            new Concatenation(new Token("a"), new Token("b")),
            new Token("c")
        );
        assertEquals("(τ[a]τ[b])|τ[c]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence5() {
        // (a|b)* requires parentheses
        Expression expr = new KleeneStar(
            new Union(new Token("a"), new Token("b"))
        );
        assertEquals("(τ[a]|τ[b])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence6() {
        // a*|b - star doesn't need parens
        Expression expr = new Union(
            new KleeneStar(new Token("a")),
            new Token("b")
        );
        assertEquals("τ[a]*|τ[b]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printMixedPrecedence7() {
        // a*b does not require parentheses around a*
        Expression expr = new Concatenation(
            new KleeneStar(new Token("a")),
            new Token("b")
        );
        assertEquals("τ[a]*τ[b]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printComplexExpression1() {
        // (a|b)*c
        Expression expr = new Concatenation(
            new KleeneStar(new Union(new Token("a"), new Token("b"))),
            new Token("c")
        );
        assertEquals("(τ[a]|τ[b])*τ[c]", PrettyPrinter.print(expr));
    }
    
    @Test
    void printComplexExpression2() {
        // a(b|c)*
        Expression expr = new Concatenation(
            new Token("a"),
            new KleeneStar(new Union(new Token("b"), new Token("c")))
        );
        assertEquals("τ[a](τ[b]|τ[c])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printComplexExpression3() {
        // (ab)* 
        Expression expr = new KleeneStar(
            new Concatenation(new Token("a"), new Token("b"))
        );
        assertEquals("(τ[a]τ[b])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printComplexExpression4() {
        // a*b*
        Expression expr = new Concatenation(
            new KleeneStar(new Token("a")),
            new KleeneStar(new Token("b"))
        );
        assertEquals("τ[a]*τ[b]*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printComplexExpression5() {
        // (a*b)*
        Expression expr = new KleeneStar(
            new Concatenation(
                new KleeneStar(new Token("a")),
                new Token("b")
            )
        );
        assertEquals("(τ[a]*τ[b])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printDeeplyNestedExpression() {
        // ((a|b)|(c|d))*
        Expression expr = new KleeneStar(
            new Union(
                new Union(new Token("a"), new Token("b")),
                new Union(new Token("c"), new Token("d"))
            )
        );
        assertEquals("(τ[a]|τ[b]|τ[c]|τ[d])*", PrettyPrinter.print(expr));
    }
    
    @Test
    void printExplicitConcatenationComplex() {
        // (a⋅b)|(c⋅d)
        Expression expr = new Union(
            new Concatenation(new Token("a"), new Token("b")),
            new Concatenation(new Token("c"), new Token("d"))
        );
        assertEquals("τ[a]⋅τ[b]|τ[c]⋅τ[d]", PrettyPrinter.print(expr, true));
    }
}
