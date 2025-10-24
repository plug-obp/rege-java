package rege.syntax;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Range record.
 */
class RangeTest {
    
    @Test
    void testValidRange() {
        Position start = new Position(1, 1, 0);
        Position end = new Position(1, 5, 4);
        Range range = new Range(start, end);
        
        assertEquals(start, range.start());
        assertEquals(end, range.end());
    }
    
    @Test
    void testZeroWidthRange() {
        Position pos = new Position(1, 5, 4);
        Range range = new Range(pos, pos);
        
        assertEquals(pos, range.start());
        assertEquals(pos, range.end());
        assertEquals(0, range.length());
    }
    
    @Test
    void testAtFactory() {
        Position pos = new Position(2, 3, 10);
        Range range = Range.at(pos);
        
        assertEquals(pos, range.start());
        assertEquals(pos, range.end());
        assertEquals(0, range.length());
    }
    
    @Test
    void testInvalidRange() {
        Position start = new Position(2, 1, 10);
        Position end = new Position(1, 5, 4);
        
        assertThrows(IllegalArgumentException.class, () -> new Range(start, end));
    }
    
    @Test
    void testNullStart() {
        Position end = new Position(1, 5, 4);
        assertThrows(NullPointerException.class, () -> new Range(null, end));
    }
    
    @Test
    void testNullEnd() {
        Position start = new Position(1, 1, 0);
        assertThrows(NullPointerException.class, () -> new Range(start, null));
    }
    
    @Test
    void testContains() {
        Range range = new Range(new Position(1, 1, 0), new Position(1, 10, 9));
        
        assertTrue(range.contains(new Position(1, 1, 0))); // start
        assertTrue(range.contains(new Position(1, 5, 4))); // middle
        assertFalse(range.contains(new Position(1, 10, 9))); // end (exclusive)
        assertFalse(range.contains(new Position(2, 1, 10))); // after
    }
    
    @Test
    void testLength() {
        Range range = new Range(new Position(1, 1, 0), new Position(1, 5, 4));
        assertEquals(4, range.length());
        
        Range zeroWidth = Range.at(new Position(1, 1, 0));
        assertEquals(0, zeroWidth.length());
    }
    
    @Test
    void testToStringZeroWidth() {
        Position pos = new Position(1, 5, 4);
        Range range = Range.at(pos);
        String str = range.toString();
        
        assertFalse(str.contains("-"));
    }
    
    @Test
    void testToStringNormal() {
        Range range = new Range(new Position(1, 1, 0), new Position(1, 5, 4));
        String str = range.toString();
        
        assertTrue(str.contains("-"));
    }
    
    @Test
    void testEquals() {
        Position p1 = new Position(1, 1, 0);
        Position p2 = new Position(1, 5, 4);
        
        Range r1 = new Range(p1, p2);
        Range r2 = new Range(p1, p2);
        Range r3 = new Range(p1, new Position(1, 6, 5));
        
        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
