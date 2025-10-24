package rege.syntax;

import org.junit.jupiter.api.Test;
import rege.syntax.model.Expression;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ParseResult sealed interface.
 */
class ParseResultTest {
    
    private static final Expression EXPR_A = new rege.syntax.model.Token("a");
    private static final Expression EXPR_B = new rege.syntax.model.Token("b");
    private static final Range RANGE = Range.at(new Position(1, 1, 0));
    private static final ParseError ERROR = new ParseError(RANGE, "Test error");
    
    @Test
    void testSuccessCreation() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
    
    @Test
    void testSuccessNullExpression() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Success(null));
    }
    
    @Test
    void testFailureCreation() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
    }
    
    @Test
    void testFailureEmptyErrors() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ParseResult.Failure(List.of(), "source"));
    }
    
    @Test
    void testFailureNullErrors() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Failure(null, "source"));
    }
    
    @Test
    void testFailureNullSource() {
        assertThrows(NullPointerException.class, 
            () -> new ParseResult.Failure(List.of(ERROR), null));
    }
    
    @Test
    void testOrElseThrowSuccess() throws ParseException {
        ParseResult result = new ParseResult.Success(EXPR_A);
        Expression expr = result.orElseThrow();
        
        assertEquals(EXPR_A, expr);
    }
    
    @Test
    void testOrElseThrowFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        
        ParseException ex = assertThrows(ParseException.class, result::orElseThrow);
        assertEquals(1, ex.getErrors().size());
        assertEquals(ERROR, ex.getErrors().get(0));
        assertEquals("source", ex.getSource());
    }
    
    @Test
    void testToOptionalSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        Optional<Expression> opt = result.toOptional();
        
        assertTrue(opt.isPresent());
        assertEquals(EXPR_A, opt.get());
    }
    
    @Test
    void testToOptionalFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        Optional<Expression> opt = result.toOptional();
        
        assertFalse(opt.isPresent());
    }
    
    @Test
    void testMapSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        ParseResult mapped = result.map(expr -> expr.union(EXPR_B));
        
        assertTrue(mapped.isSuccess());
        Expression mappedExpr = ((ParseResult.Success) mapped).expression();
        assertTrue(mappedExpr instanceof rege.syntax.model.Union);
    }
    
    @Test
    void testMapFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        ParseResult mapped = result.map(expr -> expr.union(EXPR_B));
        
        assertTrue(mapped.isFailure());
        assertEquals(result, mapped); // same failure
    }
    
    @Test
    void testMapNullFunction() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        assertThrows(NullPointerException.class, () -> result.map(null));
    }
    
    @Test
    void testFlatMapSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        ParseResult flatMapped = result.flatMap(expr -> 
            new ParseResult.Success(expr.union(EXPR_B))
        );
        
        assertTrue(flatMapped.isSuccess());
    }
    
    @Test
    void testFlatMapSuccessToFailure() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        ParseResult flatMapped = result.flatMap(expr -> 
            new ParseResult.Failure(List.of(ERROR), "source")
        );
        
        assertTrue(flatMapped.isFailure());
    }
    
    @Test
    void testFlatMapFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        ParseResult flatMapped = result.flatMap(expr -> 
            new ParseResult.Success(expr.union(EXPR_B))
        );
        
        assertTrue(flatMapped.isFailure());
        assertEquals(result, flatMapped); // same failure
    }
    
    @Test
    void testFlatMapNullFunction() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        assertThrows(NullPointerException.class, () -> result.flatMap(null));
    }
    
    @Test
    void testOrElseSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        Expression expr = result.orElse(EXPR_B);
        
        assertEquals(EXPR_A, expr);
    }
    
    @Test
    void testOrElseFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        Expression expr = result.orElse(EXPR_B);
        
        assertEquals(EXPR_B, expr);
    }
    
    @Test
    void testFormatErrorsSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        String formatted = result.formatErrors();
        
        assertEquals("", formatted);
    }
    
    @Test
    void testFormatErrorsFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "abc");
        String formatted = result.formatErrors();
        
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("Test error"));
    }
    
    @Test
    void testPatternMatchingSuccess() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        
        String outcome = switch (result) {
            case ParseResult.Success(var expr) -> "success: " + expr;
            case ParseResult.Failure(var errors, var source) -> "failure";
        };
        
        assertTrue(outcome.startsWith("success:"));
    }
    
    @Test
    void testPatternMatchingFailure() {
        ParseResult result = new ParseResult.Failure(List.of(ERROR), "source");
        
        String outcome = switch (result) {
            case ParseResult.Success(var expr) -> "success";
            case ParseResult.Failure(var errors, var source) -> 
                "failure: " + errors.size() + " errors";
        };
        
        assertEquals("failure: 1 errors", outcome);
    }
    
    @Test
    void testFailureWithMultipleErrors() {
        ParseError error1 = new ParseError(RANGE, "Error 1");
        ParseError error2 = new ParseError(RANGE, "Error 2");
        ParseResult result = new ParseResult.Failure(List.of(error1, error2), "source");
        
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertEquals(2, failure.errors().size());
    }
    
    @Test
    void testChainedMapOperations() {
        ParseResult result = new ParseResult.Success(EXPR_A);
        
        ParseResult chained = result
            .map(expr -> expr.union(EXPR_B))
            .map(expr -> expr.star());
        
        assertTrue(chained.isSuccess());
        Expression expr = ((ParseResult.Success) chained).expression();
        assertTrue(expr instanceof rege.syntax.model.KleeneStar);
    }
}
