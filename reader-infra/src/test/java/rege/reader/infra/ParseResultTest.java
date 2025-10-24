package rege.reader.infra;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ParseResult sealed interface.
 * Uses String as the generic type for testing since this is infrastructure.
 */
class ParseResultTest {
    
    private static final String VALUE_A = "value_a";
    private static final String VALUE_B = "value_b";
    private static final Range RANGE = Range.at(new Position(1, 1, 0));
    private static final ParseError ERROR = new ParseError(RANGE, "Test error");
    
    @Test
    void testSuccessCreation() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
    
    @Test
    void testSuccessNullValue() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Success<String>(null));
    }
    
    @Test
    void testFailureCreation() {
        ParseResult<String> result = new ParseResult.Failure<>(List.of(ERROR), "source");
        
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
    }
    
    @Test
    void testFailureEmptyErrors() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ParseResult.Failure<String>(List.of(), "source"));
    }
    
    @Test
    void testFailureNullErrors() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Failure<String>(null, "source"));
    }
    
    @Test
    void testFailureNullSource() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Failure<String>(List.of(ERROR), null));
    }
    
    @Test
    void testOrElseThrowSuccess() throws ParseException {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        String value = result.orElseThrow();
        
        assertEquals(VALUE_A, value);
    }
    
    @Test
    void testOrElseThrowFailure() {
        ParseResult<String> result = new ParseResult.Failure<String>(List.of(ERROR), "source");
        
        ParseException ex = assertThrows(ParseException.class, result::orElseThrow);
        assertEquals(1, ex.getErrors().size());
        assertEquals(ERROR, ex.getErrors().get(0));
        assertEquals("source", ex.getSource());
    }
    
    @Test
    void testToOptionalSuccess() {
        ParseResult<String> result = new ParseResult.Success<String>(VALUE_A);
        Optional<String> opt = result.toOptional();
        
        assertTrue(opt.isPresent());
        assertEquals(VALUE_A, opt.get());
    }
    
    @Test
    void testToOptionalFailure() {
        ParseResult<String> result = new ParseResult.Failure<String>(List.of(ERROR), "source");
        Optional<String> opt = result.toOptional();
        
        assertFalse(opt.isPresent());
    }
    
    @Test
    void testMapSuccess() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        ParseResult<String> mapped = result.map(s -> s.toUpperCase());
        
        assertTrue(mapped.isSuccess());
        String mappedValue = ((ParseResult.Success<String>) mapped).value();
        assertEquals("VALUE_A", mappedValue);
    }
    
    @Test
    void testMapFailure() {
        ParseResult<String> result = new ParseResult.Failure<>(List.of(ERROR), "source");
        ParseResult<String> mapped = result.map(s -> s.toUpperCase());
        
        assertTrue(mapped.isFailure());
        assertEquals(result, mapped); // same failure
    }
    
    @Test
    void testMapNullFunction() {
        ParseResult<String> result = new ParseResult.Success<String>(VALUE_A);
        assertThrows(NullPointerException.class, () -> result.map(null));
    }
    
    @Test
    void testFlatMapSuccess() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        ParseResult<String> flatMapped = result.flatMap(s -> 
            new ParseResult.Success<>(s + "_" + VALUE_B)
        );
        
        assertTrue(flatMapped.isSuccess());
    }
    
    @Test
    void testFlatMapSuccessToFailure() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        ParseResult<String> flatMapped = result.flatMap(s -> 
            new ParseResult.Failure<>(List.of(ERROR), "source")
        );
        
        assertTrue(flatMapped.isFailure());
    }
    
    @Test
    void testFlatMapFailure() {
        ParseResult<String> result = new ParseResult.Failure<>(List.of(ERROR), "source");
        ParseResult<String> flatMapped = result.flatMap(s -> 
            new ParseResult.Success<>(s + "_" + VALUE_B)
        );
        
        assertTrue(flatMapped.isFailure());
        assertEquals(result, flatMapped); // same failure
    }
    
    @Test
    void testFlatMapNullFunction() {
        ParseResult<String> result = new ParseResult.Success<String>(VALUE_A);
        assertThrows(NullPointerException.class, () -> result.flatMap(null));
    }
    
    @Test
    void testOrElseSuccess() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        String value = result.orElse(VALUE_B);
        
        assertEquals(VALUE_A, value);
    }
    
    @Test
    void testOrElseFailure() {
        ParseResult<String> result = new ParseResult.Failure<>(List.of(ERROR), "source");
        String value = result.orElse(VALUE_B);
        
        assertEquals(VALUE_B, value);
    }
    
    @Test
    void testFormatErrorsSuccess() {
        ParseResult<String> result = new ParseResult.Success<String>(VALUE_A);
        String formatted = result.formatErrors();
        
        assertEquals("", formatted);
    }
    
    @Test
    void testFormatErrorsFailure() {
        ParseResult<String> result = new ParseResult.Failure<String>(List.of(ERROR), "abc");
        String formatted = result.formatErrors();
        
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("Test error"));
    }
    
    @Test
    void testPatternMatchingSuccess() {
        ParseResult<String> result = new ParseResult.Success<String>(VALUE_A);
        
        String outcome = switch (result) {
            case ParseResult.Success<String>(var expr) -> "success: " + expr;
            case ParseResult.Failure<String>(var errors, var source) -> "failure";
        };
        
        assertTrue(outcome.startsWith("success:"));
    }
    
    @Test
    void testPatternMatchingFailure() {
        ParseResult<String> result = new ParseResult.Failure<String>(List.of(ERROR), "source");
        
        String outcome = switch (result) {
            case ParseResult.Success<String>(var expr) -> "success";
            case ParseResult.Failure<String>(var errors, var source) -> 
                "failure: " + errors.size() + " errors";
        };
        
        assertEquals("failure: 1 errors", outcome);
    }
    
    @Test
    void testFailureWithMultipleErrors() {
        ParseError error1 = new ParseError(RANGE, "Error 1");
        ParseError error2 = new ParseError(RANGE, "Error 2");
        ParseResult<String> result = new ParseResult.Failure<String>(List.of(error1, error2), "source");
        
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals(2, failure.errors().size());
    }
    
    @Test
    void testChainedMapOperations() {
        ParseResult<String> result = new ParseResult.Success<>(VALUE_A);
        
        ParseResult<String> chained = result
            .map(s -> s + "_" + VALUE_B)
            .map(s -> s + "*");
        
        assertTrue(chained.isSuccess());
        String value = ((ParseResult.Success<String>) chained).value();
        assertTrue(value.endsWith("*"));
    }
}
