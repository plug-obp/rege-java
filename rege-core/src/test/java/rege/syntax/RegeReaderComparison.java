package rege.syntax;

import rege.syntax.model.*;

/**
 * Demonstration of the difference between RegeReader (right-associative)
 * and RegeReaderLeft (left-associative) parsers.
 * 
 * Both parsers produce semantically equivalent expressions, but with different
 * AST structures. This affects how the expressions are traversed and processed.
 */
public class RegeReaderComparison {
    
    public static void main(String[] args) {
        demonstrateConcatenation();
        System.out.println();
        demonstrateUnion();
        System.out.println();
        demonstrateComplexExpression();
        System.out.println();
        demonstrateSemanticEquivalence();
    }
    
    private static void demonstrateConcatenation() {
        String input = "τ[a].τ[b].τ[c]";
        System.out.println("Input: " + input);
        System.out.println("================");
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        System.out.println("RegeReader (right-associative):");
        System.out.println("  Structure: a⋅(b⋅c)");
        System.out.println("  Tree: " + treeStructure(right, 2));
        
        System.out.println("\nRegeReaderLeft (left-associative):");
        System.out.println("  Structure: (a⋅b)⋅c");
        System.out.println("  Tree: " + treeStructure(left, 2));
        
        System.out.println("\nAre they equal? " + right.equals(left));
        System.out.println("(They're semantically equivalent but structurally different)");
    }
    
    private static void demonstrateUnion() {
        String input = "τ[a]|τ[b]|τ[c]|τ[d]";
        System.out.println("Input: " + input);
        System.out.println("================");
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        System.out.println("RegeReader (right-associative):");
        System.out.println("  Structure: a|(b|(c|d))");
        System.out.println("  Tree: " + treeStructure(right, 2));
        
        System.out.println("\nRegeReaderLeft (left-associative):");
        System.out.println("  Structure: ((a|b)|c)|d");
        System.out.println("  Tree: " + treeStructure(left, 2));
    }
    
    private static void demonstrateComplexExpression() {
        String input = "(τ[a]|τ[b]).τ[c]*.τ[d]";
        System.out.println("Input: " + input);
        System.out.println("================");
        
        Expression right = RegeReader.readExpression(input, false);
        Expression left = RegeReaderLeft.readExpression(input, false);
        
        System.out.println("RegeReader (right-associative):");
        System.out.println("  Structure: (a|b)⋅(c*⋅d)");
        System.out.println("  Tree: " + treeStructure(right, 2));
        
        System.out.println("\nRegeReaderLeft (left-associative):");
        System.out.println("  Structure: ((a|b)⋅c*)⋅d");
        System.out.println("  Tree: " + treeStructure(left, 2));
    }
    
    private static void demonstrateSemanticEquivalence() {
        System.out.println("Semantic Equivalence with Smart Constructors");
        System.out.println("==============================================");
        
        // Test cases where smart constructors produce identical results
        String[] tests = {
            "∅*",          // Both: ε
            "ϵ*",          // Both: ε
            "∅|τ[a]",      // Both: a
            "τ[a]|∅",      // Both: a
            "ϵ.τ[a]",      // Both: a
            "τ[a].ϵ"       // Both: a
        };
        
        for (String test : tests) {
            Expression right = RegeReader.readExpression(test, true);
            Expression left = RegeReaderLeft.readExpression(test, true);
            boolean equal = right.equals(left);
            System.out.printf("  %-15s → Right: %-10s Left: %-10s Equal: %s%n",
                test, 
                simpleName(right), 
                simpleName(left), 
                equal ? "✓" : "✗"
            );
        }
    }
    
    /**
     * Generate a simple tree structure representation.
     */
    private static String treeStructure(Expression expr, int indent) {
        String spaces = " ".repeat(indent);
        
        if (expr instanceof Token token) {
            return "Token(" + token.value() + ")";
        } else if (expr instanceof Concatenation concat) {
            return "Concat(\n" + 
                spaces + "  " + treeStructure(concat.lhs(), indent + 2) + ",\n" +
                spaces + "  " + treeStructure(concat.rhs(), indent + 2) + "\n" +
                spaces + ")";
        } else if (expr instanceof Union union) {
            return "Union(\n" + 
                spaces + "  " + treeStructure(union.lhs(), indent + 2) + ",\n" +
                spaces + "  " + treeStructure(union.rhs(), indent + 2) + "\n" +
                spaces + ")";
        } else if (expr instanceof KleeneStar star) {
            return "Star(" + treeStructure(star.expression(), indent + 2) + ")";
        } else if (expr == Expression.EMPTY) {
            return "∅";
        } else if (expr == Expression.EPSILON) {
            return "ε";
        }
        
        return expr.toString();
    }
    
    /**
     * Get simple name for an expression.
     */
    private static String simpleName(Expression expr) {
        if (expr instanceof Token token) {
            return "τ[" + token.value() + "]";
        } else if (expr == Expression.EMPTY) {
            return "∅";
        } else if (expr == Expression.EPSILON) {
            return "ε";
        } else if (expr instanceof Concatenation) {
            return "Concat(...)";
        } else if (expr instanceof Union) {
            return "Union(...)";
        } else if (expr instanceof KleeneStar) {
            return "Star(...)";
        }
        return expr.getClass().getSimpleName();
    }
}
