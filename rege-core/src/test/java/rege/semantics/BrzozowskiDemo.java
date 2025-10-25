package rege.semantics;

import rege.syntax.RegeReader;
import rege.syntax.model.*;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Demonstration of Brzozowski derivatives and dependent semantics.
 */
public class BrzozowskiDemo {
    
    public static void main(String[] args) {
        demonstrateBasicDerivatives();
        System.out.println();
        demonstrateStringMatching();
        System.out.println();
        demonstrateDependentSemantics();
        System.out.println();
        demonstrateStepByStepDerivation();
    }
    
    private static void demonstrateBasicDerivatives() {
        System.out.println("=== Basic Brzozowski Derivatives ===");
        
        BiPredicate<String, Character> charEval = (token, ch) -> token.equals(String.valueOf(ch));
        Brzozowski<Character> brz = new Brzozowski<>(charEval);
        
        // Simple examples
        Expression a = new Token("a");
        Expression b = new Token("b");
        
        System.out.println("D_a(τ[a]) = " + formatExpr(brz.derivative(a, 'a')));  // ε
        System.out.println("D_a(τ[b]) = " + formatExpr(brz.derivative(b, 'a')));  // ∅
        System.out.println("D_a(∅) = " + formatExpr(brz.derivative(Expression.EMPTY, 'a')));  // ∅
        System.out.println("D_a(ε) = " + formatExpr(brz.derivative(Expression.EPSILON, 'a')));  // ∅
        
        // Union
        Expression union = new Union(a, b);
        System.out.println("D_a(a|b) = " + formatExpr(brz.derivative(union, 'a')));  // ε
        
        // Concatenation
        Expression concat = new Concatenation(a, b);
        System.out.println("D_a(a⋅b) = " + formatExpr(brz.derivative(concat, 'a')));  // b
        
        // Kleene star
        Expression star = new KleeneStar(a);
        System.out.println("D_a(a*) = " + formatExpr(brz.derivative(star, 'a')));  // a*
    }
    
    private static void demonstrateStringMatching() {
        System.out.println("=== String Matching with Derivatives ===");
        
        BiPredicate<String, Character> charEval = (token, ch) -> token.equals(String.valueOf(ch));
        Brzozowski<Character> brz = new Brzozowski<>(charEval);
        
        // Pattern: (a|b)*⋅c
        Expression pattern = RegeReader.parse("(t[a]|t[b])*⋅t[c]").orElse(null);
        
        String[] testStrings = {"c", "ac", "bc", "abc", "bac", "aaabbbccc", "ab", ""};
        
        System.out.println("Pattern: (a|b)*⋅c");
        for (String test : testStrings) {
            List<Character> chars = test.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
            
            boolean matches = brz.matches(pattern, chars);
            System.out.printf("  \"%s\" → %s%n", test, matches ? "✓ MATCH" : "✗ NO MATCH");
        }
    }
    
    private static void demonstrateDependentSemantics() {
        System.out.println("=== Dependent Semantics ===");
        
        BiPredicate<String, Character> charEval = (token, ch) -> token.equals(String.valueOf(ch));
        
        // Pattern: a*⋅b*
        Expression starA = new KleeneStar(new Token("a"));
        Expression starB = new KleeneStar(new Token("b"));
        Expression pattern = new Concatenation(starA, starB);
        
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEval);
        
        String[] testStrings = {"", "a", "b", "ab", "aab", "abb", "aaabbb", "ba", "aba"};
        
        System.out.println("Pattern: a*⋅b*");
        for (String test : testStrings) {
            List<Character> chars = test.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
            
            boolean accepted = semantics.accepts(chars);
            System.out.printf("  \"%s\" → %s%n", test, accepted ? "✓ ACCEPT" : "✗ REJECT");
        }
    }
    
    private static void demonstrateStepByStepDerivation() {
        System.out.println("=== Step-by-Step Derivation ===");
        
        BiPredicate<String, Character> charEval = (token, ch) -> token.equals(String.valueOf(ch));
        Brzozowski<Character> brz = new Brzozowski<>(charEval);
        
        // Pattern: (a⋅b)*
        Expression ab = new Concatenation(new Token("a"), new Token("b"));
        Expression pattern = new KleeneStar(ab);
        
        String input = "abab";
        System.out.println("Pattern: (a⋅b)*");
        System.out.println("Input: \"" + input + "\"");
        System.out.println();
        
        Expression current = pattern;
        System.out.printf("%-10s | %-30s | Nullable?%n", "Step", "Expression", "");
        System.out.println("-".repeat(60));
        System.out.printf("%-10s | %-30s | %s%n", 
            "initial", formatExpr(current), brz.isNullable(current) ? "YES" : "NO");
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            current = brz.derivative(current, ch);
            System.out.printf("%-10s | %-30s | %s%n", 
                "D_" + ch, formatExpr(current), brz.isNullable(current) ? "YES" : "NO");
        }
        
        System.out.println();
        System.out.println("Final: " + (brz.isNullable(current) ? "✓ ACCEPTED" : "✗ REJECTED"));
    }
    
    private static String formatExpr(Expression expr) {
        if (expr == Expression.EMPTY) return "∅";
        if (expr == Expression.EPSILON) return "ε";
        if (expr instanceof Token t) return "τ[" + t.value() + "]";
        if (expr instanceof Union u) return "(" + formatExpr(u.lhs()) + "|" + formatExpr(u.rhs()) + ")";
        if (expr instanceof Concatenation c) return "(" + formatExpr(c.lhs()) + "⋅" + formatExpr(c.rhs()) + ")";
        if (expr instanceof KleeneStar s) return "(" + formatExpr(s.expression()) + ")*";
        return expr.toString();
    }
}
