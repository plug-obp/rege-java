package rege.syntax;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Peekable class with position tracking.
 */
class PeekableTest {
    
    @Test
    void testBasicIteration() {
        Peekable peekable = new Peekable("abc");
        
        assertTrue(peekable.hasNext());
        assertEquals('a', peekable.next());
        assertEquals('b', peekable.next());
        assertEquals('c', peekable.next());
        assertFalse(peekable.hasNext());
    }
    
    @Test
    void testPeek() {
        Peekable peekable = new Peekable("abc");
        
        assertEquals('a', peekable.peek());
        assertEquals('a', peekable.peek()); // peek doesn't consume
        assertEquals('a', peekable.next()); // now consume
        assertEquals('b', peekable.peek());
    }
    
    @Test
    void testNextWhenEmpty() {
        Peekable peekable = new Peekable("");
        
        assertThrows(NoSuchElementException.class, peekable::next);
    }
    
    @Test
    void testPeekWhenEmpty() {
        Peekable peekable = new Peekable("");
        
        assertThrows(NoSuchElementException.class, peekable::peek);
    }
    
    @Test
    void testInitialPosition() {
        Peekable peekable = new Peekable("abc");
        Position pos = peekable.position();
        
        assertEquals(1, pos.line());
        assertEquals(1, pos.column());
        assertEquals(0, pos.offset());
    }
    
    @Test
    void testPositionAdvancesSingleLine() {
        Peekable peekable = new Peekable("abc");
        
        peekable.next(); // 'a'
        Position pos = peekable.position();
        assertEquals(1, pos.line());
        assertEquals(2, pos.column());
        assertEquals(1, pos.offset());
        
        peekable.next(); // 'b'
        pos = peekable.position();
        assertEquals(1, pos.line());
        assertEquals(3, pos.column());
        assertEquals(2, pos.offset());
    }
    
    @Test
    void testPositionWithNewline() {
        Peekable peekable = new Peekable("ab\ncd");
        
        peekable.next(); // 'a'
        peekable.next(); // 'b'
        peekable.next(); // '\n'
        
        Position pos = peekable.position();
        assertEquals(2, pos.line());
        assertEquals(1, pos.column());
        assertEquals(3, pos.offset());
        
        peekable.next(); // 'c'
        pos = peekable.position();
        assertEquals(2, pos.line());
        assertEquals(2, pos.column());
        assertEquals(4, pos.offset());
    }
    
    @Test
    void testPositionMultipleNewlines() {
        Peekable peekable = new Peekable("a\n\nb");
        
        peekable.next(); // 'a'
        peekable.next(); // '\n'
        Position pos = peekable.position();
        assertEquals(2, pos.line());
        assertEquals(1, pos.column());
        
        peekable.next(); // '\n'
        pos = peekable.position();
        assertEquals(3, pos.line());
        assertEquals(1, pos.column());
        
        peekable.next(); // 'b'
        pos = peekable.position();
        assertEquals(3, pos.line());
        assertEquals(2, pos.column());
    }
    
    @Test
    void testRangeFrom() {
        Peekable peekable = new Peekable("hello");
        Position start = peekable.position();
        
        peekable.next(); // 'h'
        peekable.next(); // 'e'
        peekable.next(); // 'l'
        peekable.next(); // 'l'
        peekable.next(); // 'o'
        
        Range range = peekable.rangeFrom(start);
        assertEquals(start, range.start());
        assertEquals(peekable.position(), range.end());
        assertEquals(5, range.length());
    }
    
    @Test
    void testRangeHere() {
        Peekable peekable = new Peekable("abc");
        peekable.next(); // 'a'
        
        Range range = peekable.rangeHere();
        Position pos = peekable.position();
        
        assertEquals(pos, range.start());
        assertEquals(pos, range.end());
        assertEquals(0, range.length());
    }
    
    @Test
    void testSource() {
        String input = "test input";
        Peekable peekable = new Peekable(input);
        
        assertEquals(input, peekable.source());
    }
    
    @Test
    void testRangeAcrossLines() {
        Peekable peekable = new Peekable("ab\ncd");
        Position start = peekable.position();
        
        peekable.next(); // 'a'
        peekable.next(); // 'b'
        peekable.next(); // '\n'
        peekable.next(); // 'c'
        
        Range range = peekable.rangeFrom(start);
        assertEquals(1, range.start().line());
        assertEquals(1, range.start().column());
        assertEquals(2, range.end().line());
        assertEquals(2, range.end().column());
        assertEquals(4, range.length());
    }
    
    @Test
    void testEmptyString() {
        Peekable peekable = new Peekable("");
        
        assertFalse(peekable.hasNext());
        Position pos = peekable.position();
        assertEquals(1, pos.line());
        assertEquals(1, pos.column());
        assertEquals(0, pos.offset());
    }
}
