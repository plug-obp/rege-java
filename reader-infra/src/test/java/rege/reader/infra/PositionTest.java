package rege.reader.infra;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Position record.
 */
class PositionTest {
    
    @Test
    void testValidPosition() {
        Position pos = new Position(0, 0, 0);
        assertEquals(0, pos.line());
        assertEquals(0, pos.column());
        assertEquals(0, pos.offset());
    }
    
    @Test
    void testInvalidLine() {
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 1, 0));
    }
    
    @Test
    void testInvalidColumn() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(1, -1, 0));
    }
    
    @Test
    void testInvalidOffset() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 0, -1));
    }
    
    @Test
    void testCompareTo() {
        Position p1 = new Position(0, 0, 0);
        Position p2 = new Position(0, 4, 4);
        Position p3 = new Position(1, 0, 10);
        
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
        assertEquals(0, p1.compareTo(p1));
        
        assertTrue(p2.compareTo(p3) < 0);
        assertTrue(p3.compareTo(p2) > 0);
    }
    
    @Test
    void testStartFactory() {
        Position start = Position.start();
        assertEquals(0, start.line());
        assertEquals(0, start.column());
        assertEquals(0, start.offset());
    }
    
    @Test
    void testToString() {
        Position pos = new Position(2, 6, 25);
        String str = pos.toString();
        assertTrue(str.contains("2"));
        assertTrue(str.contains("6"));
    }
    
    @Test
    void testEquals() {
        Position p1 = new Position(0, 4, 4);
        Position p2 = new Position(0, 4, 4);
        Position p3 = new Position(0, 5, 5);
        
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
