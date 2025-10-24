package rege.reader.infra;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ParseError record.
 */
class ParseErrorTest {
    
    private static final Position POS = new Position(1, 5, 4);
    private static final Range RANGE = Range.at(POS);
    
    @Test
    void testBasicConstructor() {
        ParseError error = new ParseError(RANGE, "Unexpected character");
        
        assertEquals(RANGE, error.range());
        assertEquals("Unexpected character", error.message());
        assertEquals(ParseError.Severity.ERROR, error.severity());
        assertEquals(Optional.empty(), error.code());
    }
    
    @Test
    void testConstructorWithSeverity() {
        ParseError error = new ParseError(RANGE, "This is a warning", ParseError.Severity.WARNING);
        
        assertEquals(ParseError.Severity.WARNING, error.severity());
    }
    
    @Test
    void testConstructorWithCode() {
        ParseError error = new ParseError(RANGE, "Unexpected character", "unexpected-char");
        
        assertEquals(Optional.of("unexpected-char"), error.code());
    }
    
    @Test
    void testFullConstructor() {
        ParseError error = new ParseError(
            RANGE,
            "Hint message",
            ParseError.Severity.HINT,
            Optional.of("hint-code")
        );
        
        assertEquals(ParseError.Severity.HINT, error.severity());
        assertEquals(Optional.of("hint-code"), error.code());
    }
    
    @Test
    void testNullRange() {
        assertThrows(NullPointerException.class, 
            () -> new ParseError(null, "message"));
    }
    
    @Test
    void testNullMessage() {
        assertThrows(NullPointerException.class, 
            () -> new ParseError(RANGE, null));
    }
    
    @Test
    void testBlankMessage() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ParseError(RANGE, ""));
        assertThrows(IllegalArgumentException.class, 
            () -> new ParseError(RANGE, "   "));
    }
    
    @Test
    void testNullSeverity() {
        assertThrows(NullPointerException.class, 
            () -> new ParseError(RANGE, "message", (ParseError.Severity) null));
    }
    
    @Test
    void testToStringWithoutCode() {
        ParseError error = new ParseError(RANGE, "Test error");
        String str = error.toString();
        
        assertTrue(str.contains("error"));
        assertTrue(str.contains("Test error"));
        assertFalse(str.contains("["));
    }
    
    @Test
    void testToStringWithCode() {
        ParseError error = new ParseError(RANGE, "Test error", "test-code");
        String str = error.toString();
        
        assertTrue(str.contains("Test error"));
        assertTrue(str.contains("[test-code]"));
    }
    
    @Test
    void testFormatWithSource() {
        Position start = new Position(2, 5, 10);
        Position end = new Position(2, 6, 11);
        Range range = new Range(start, end);
        ParseError error = new ParseError(range, "Unexpected character '}'");
        
        String source = "abc\ntest}xyz\nmore";
        String formatted = error.formatWithSource(source);
        
        assertTrue(formatted.contains("Unexpected character '}'"));
        assertTrue(formatted.contains("test}xyz"));
        assertTrue(formatted.contains("^"));
    }
    
    @Test
    void testFormatWithSourceMultiCharError() {
        Position start = new Position(1, 1, 0);
        Position end = new Position(1, 6, 5);
        Range range = new Range(start, end);
        ParseError error = new ParseError(range, "Invalid token");
        
        String source = "hello world";
        String formatted = error.formatWithSource(source);
        
        assertTrue(formatted.contains("hello world"));
        assertTrue(formatted.contains("^^^^^"));
    }
    
    @Test
    void testSeverityLspValues() {
        assertEquals(1, ParseError.Severity.ERROR.getLspValue());
        assertEquals(2, ParseError.Severity.WARNING.getLspValue());
        assertEquals(3, ParseError.Severity.INFO.getLspValue());
        assertEquals(4, ParseError.Severity.HINT.getLspValue());
    }
    
    @Test
    void testEquals() {
        ParseError e1 = new ParseError(RANGE, "Test", "code");
        ParseError e2 = new ParseError(RANGE, "Test", "code");
        ParseError e3 = new ParseError(RANGE, "Different", "code");
        
        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
