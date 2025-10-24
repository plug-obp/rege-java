package rege.syntax;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Position record.
 */
class PositionTest {
    
    @Test
    void testValidPosition() {
        Position pos = new Position(1, 1, 0);
        assertEquals(1, pos.line());
        assertEquals(1, pos.column());
        assertEquals(0, pos.offset());
    }
    
    @Test
    void testInvalidLine() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 1, 0));
    }
    
    @Test
    void testInvalidColumn() {
        assertThrows(IllegalArgumentException.class, () -> new Position(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(1, -1, 0));
    }
    
    @Test
    void testInvalidOffset() {
        assertThrows(IllegalArgumentException.class, () -> new Position(1, 1, -1));
    }
    
    @Test
    void testCompareTo() {
        Position p1 = new Position(1, 1, 0);
        Position p2 = new Position(1, 5, 4);
        Position p3 = new Position(2, 1, 10);
        
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
        assertEquals(0, p1.compareTo(p1));
        
        assertTrue(p2.compareTo(p3) < 0);
        assertTrue(p3.compareTo(p2) > 0);
    }
    
    @Test
    void testStartFactory() {
        Position start = Position.start();
        assertEquals(1, start.line());
        assertEquals(1, start.column());
        assertEquals(0, start.offset());
    }
    
    @Test
    void testToString() {
        Position pos = new Position(3, 7, 25);
        String str = pos.toString();
        assertTrue(str.contains("3"));
        assertTrue(str.contains("7"));
    }
    
    @Test
    void testEquals() {
        Position p1 = new Position(1, 5, 4);
        Position p2 = new Position(1, 5, 4);
        Position p3 = new Position(1, 6, 5);
        
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
