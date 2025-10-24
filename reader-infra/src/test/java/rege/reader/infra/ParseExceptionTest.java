package rege.reader.infra;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ParseException class.
 */
class ParseExceptionTest {
    
    private static final Range RANGE = Range.at(new Position(1, 5, 4));
    private static final ParseError ERROR = new ParseError(RANGE, "Test error");
    
    @Test
    void testBasicConstruction() {
        ParseException ex = new ParseException(List.of(ERROR), "source");
        
        assertEquals(1, ex.getErrors().size());
        assertEquals(ERROR, ex.getErrors().get(0));
        assertEquals("source", ex.getSource());
    }
    
    @Test
    void testNullErrors() {
        assertThrows(NullPointerException.class, 
            () -> new ParseException(null, "source"));
    }
    
    @Test
    void testEmptyErrors() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ParseException(List.of(), "source"));
    }
    
    @Test
    void testNullSource() {
        assertThrows(NullPointerException.class, 
            () -> new ParseException(List.of(ERROR), null));
    }
    
    @Test
    void testMessageSingleError() {
        ParseException ex = new ParseException(List.of(ERROR), "abc");
        String message = ex.getMessage();
        
        assertTrue(message.contains("1 error"));
        assertFalse(message.contains("errors"));
        assertTrue(message.contains("Test error"));
    }
    
    @Test
    void testMessageMultipleErrors() {
        ParseError error1 = new ParseError(RANGE, "Error 1");
        ParseError error2 = new ParseError(RANGE, "Error 2");
        ParseException ex = new ParseException(List.of(error1, error2), "abc");
        String message = ex.getMessage();
        
        assertTrue(message.contains("2 errors"));
        assertTrue(message.contains("Error 1"));
        assertTrue(message.contains("Error 2"));
    }
    
    @Test
    void testMessageWithSourceContext() {
        Position start = new Position(1, 5, 4);
        Position end = new Position(1, 6, 5);
        Range range = new Range(start, end);
        ParseError error = new ParseError(range, "Unexpected character");
        
        String source = "test}more";
        ParseException ex = new ParseException(List.of(error), source);
        String message = ex.getMessage();
        
        assertTrue(message.contains("test}more"));
        assertTrue(message.contains("^"));
    }
    
    @Test
    void testErrorsAreImmutable() {
        ParseError error1 = new ParseError(RANGE, "Error 1");
        ParseError error2 = new ParseError(RANGE, "Error 2");
        List<ParseError> originalList = new java.util.ArrayList<>();
        originalList.add(error1);
        
        ParseException ex = new ParseException(originalList, "source");
        
        // Modify original list
        originalList.add(error2);
        
        // Exception should still have only 1 error (defensive copy)
        assertEquals(1, ex.getErrors().size());
    }
    
    @Test
    void testGetErrorsReturnsImmutable() {
        ParseException ex = new ParseException(List.of(ERROR), "source");
        List<ParseError> errors = ex.getErrors();
        
        assertThrows(UnsupportedOperationException.class, 
            () -> errors.add(new ParseError(RANGE, "Another error")));
    }
}
