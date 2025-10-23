package rege.semantics;

import org.junit.jupiter.api.Test;
import rege.syntax.model.*;

import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegeDependentSemantics.
 */
class RegeDependentSemanticsTest {
    
    private final BiPredicate<String, Character> charEvaluator = 
        (token, ch) -> token.equals(String.valueOf(ch));
    
    @Test
    void testInitialConfiguration() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        List<Expression> initial = semantics.initial();
        assertEquals(1, initial.size());
        assertEquals(pattern, initial.get(0));
    }
    
    @Test
    void testActionsWhenInhabited() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        List<Brzozowski<Character>> actions = semantics.actions('a', pattern);
        assertEquals(1, actions.size());
    }
    
    @Test
    void testActionsWhenNotInhabited() {
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(Expression.EMPTY, charEvaluator);
        
        List<Brzozowski<Character>> actions = semantics.actions('a', Expression.EMPTY);
        assertTrue(actions.isEmpty());
    }
    
    @Test
    void testExecute() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        Brzozowski<Character> derivator = new Brzozowski<>(charEvaluator);
        List<Expression> result = semantics.execute(derivator, 'a', pattern);
        
        assertEquals(1, result.size());
        assertEquals(Expression.EPSILON, result.get(0));
    }
    
    @Test
    void testProcessSingleCharacter() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        Expression result = semantics.process(List.of('a'));
        assertEquals(Expression.EPSILON, result);
        assertTrue(RegeDependentSemantics.isAccepting(result));
    }
    
    @Test
    void testProcessNonMatchingCharacter() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        Expression result = semantics.process(List.of('b'));
        assertEquals(Expression.EMPTY, result);
        assertFalse(RegeDependentSemantics.isAccepting(result));
    }
    
    @Test
    void testAcceptsSingleCharacter() {
        Expression pattern = new Token("a");
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        assertTrue(semantics.accepts(List.of('a')));
        assertFalse(semantics.accepts(List.of('b')));
        assertFalse(semantics.accepts(List.of()));
    }
    
    @Test
    void testAcceptsConcatenation() {
        // Pattern: a⋅b
        Expression pattern = new Concatenation(new Token("a"), new Token("b"));
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        assertTrue(semantics.accepts(List.of('a', 'b')));
        assertFalse(semantics.accepts(List.of('a')));
        assertFalse(semantics.accepts(List.of('b')));
        assertFalse(semantics.accepts(List.of('b', 'a')));
    }
    
    @Test
    void testAcceptsUnion() {
        // Pattern: a|b
        Expression pattern = new Union(new Token("a"), new Token("b"));
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        assertTrue(semantics.accepts(List.of('a')));
        assertTrue(semantics.accepts(List.of('b')));
        assertFalse(semantics.accepts(List.of('c')));
    }
    
    @Test
    void testAcceptsKleeneStar() {
        // Pattern: a*
        Expression pattern = new KleeneStar(new Token("a"));
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        assertTrue(semantics.accepts(List.of()));
        assertTrue(semantics.accepts(List.of('a')));
        assertTrue(semantics.accepts(List.of('a', 'a')));
        assertTrue(semantics.accepts(List.of('a', 'a', 'a')));
        assertFalse(semantics.accepts(List.of('b')));
    }
    
    @Test
    void testAcceptsComplexPattern() {
        // Pattern: (a|b)*⋅c
        Expression union = new Union(new Token("a"), new Token("b"));
        Expression star = new KleeneStar(union);
        Expression pattern = new Concatenation(star, new Token("c"));
        
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        assertTrue(semantics.accepts(List.of('c')));
        assertTrue(semantics.accepts(List.of('a', 'c')));
        assertTrue(semantics.accepts(List.of('b', 'c')));
        assertTrue(semantics.accepts(List.of('a', 'b', 'c')));
        assertTrue(semantics.accepts(List.of('b', 'a', 'a', 'b', 'c')));
        assertFalse(semantics.accepts(List.of()));
        assertFalse(semantics.accepts(List.of('a')));
        assertFalse(semantics.accepts(List.of('c', 'a')));
    }
    
    @Test
    void testIsAcceptingStatic() {
        assertTrue(RegeDependentSemantics.isAccepting(Expression.EPSILON));
        assertFalse(RegeDependentSemantics.isAccepting(Expression.EMPTY));
        assertFalse(RegeDependentSemantics.isAccepting(new Token("a")));
        assertTrue(RegeDependentSemantics.isAccepting(new KleeneStar(new Token("a"))));
    }
    
    @Test
    void testProcessEmptyInput() {
        // Pattern: a*
        Expression pattern = new KleeneStar(new Token("a"));
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        Expression result = semantics.process(List.of());
        assertTrue(RegeDependentSemantics.isAccepting(result));
    }
    
    @Test
    void testProcessStopsWhenLanguageBecomesEmpty() {
        // Pattern: a⋅b
        Expression pattern = new Concatenation(new Token("a"), new Token("b"));
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        // Try input "ac" - should fail at 'c'
        Expression result = semantics.process(List.of('a', 'c'));
        assertEquals(Expression.EMPTY, result);
        assertFalse(RegeDependentSemantics.isAccepting(result));
    }
    
    @Test
    void testManualStepByStep() {
        // Pattern: a⋅b⋅c
        Expression ab = new Concatenation(new Token("a"), new Token("b"));
        Expression pattern = new Concatenation(ab, new Token("c"));
        
        RegeDependentSemantics<Character> semantics = 
            new RegeDependentSemantics<>(pattern, charEvaluator);
        
        // Initial
        List<Expression> configs = semantics.initial();
        assertEquals(pattern, configs.get(0));
        assertFalse(RegeDependentSemantics.isAccepting(configs.get(0)));
        
        // Process 'a'
        List<Brzozowski<Character>> actions1 = semantics.actions('a', configs.get(0));
        assertEquals(1, actions1.size());
        configs = semantics.execute(actions1.get(0), 'a', configs.get(0));
        assertFalse(RegeDependentSemantics.isAccepting(configs.get(0)));
        
        // Process 'b'
        List<Brzozowski<Character>> actions2 = semantics.actions('b', configs.get(0));
        assertEquals(1, actions2.size());
        configs = semantics.execute(actions2.get(0), 'b', configs.get(0));
        assertFalse(RegeDependentSemantics.isAccepting(configs.get(0)));
        
        // Process 'c'
        List<Brzozowski<Character>> actions3 = semantics.actions('c', configs.get(0));
        assertEquals(1, actions3.size());
        configs = semantics.execute(actions3.get(0), 'c', configs.get(0));
        assertTrue(RegeDependentSemantics.isAccepting(configs.get(0)));
    }
    
    @Test
    void testWithCustomDerivatorBuilder() {
        Expression pattern = new Token("a");
        
        // Custom builder that creates a new Brzozowski each time
        RegeDependentSemantics<Character> semantics = new RegeDependentSemantics<>(
            pattern,
            Brzozowski::new,
            charEvaluator
        );
        
        assertTrue(semantics.accepts(List.of('a')));
        assertFalse(semantics.accepts(List.of('b')));
    }
}
