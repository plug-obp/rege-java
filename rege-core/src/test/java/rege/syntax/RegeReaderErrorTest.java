package rege.syntax;

import org.junit.jupiter.api.Test;
import rege.syntax.model.Expression;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegeReader error reporting with ParseResult.
 */
class RegeReaderErrorTest {
    
    @Test
    void testSuccessfulParse() {
        ParseResult result = RegeReader.parse("τ[a]");
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
    
    @Test
    void testSuccessfulParsePattern() {
        ParseResult result = RegeReader.parse("τ[a]|τ[b]");
        
        switch (result) {
            case ParseResult.Success(var expr) -> assertNotNull(expr);
            case ParseResult.Failure(var errors, var source) -> 
                fail("Expected success but got " + errors.size() + " errors");
        }
    }
    
    @Test
    void testOrElseThrow() throws ParseException {
        Expression expr = RegeReader.parse("τ[a]").orElseThrow();
        assertNotNull(expr);
    }
    
    @Test
    void testOrElseThrowFailure() {
        ParseResult result = RegeReader.parse("τ[unclosed");
        
        ParseException ex = assertThrows(ParseException.class, result::orElseThrow);
        assertFalse(ex.getErrors().isEmpty());
    }
    
    @Test
    void testUnclosedToken() {
        ParseResult result = RegeReader.parse("τ[hello");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertEquals(1, failure.errors().size());
        assertTrue(failure.errors().get(0).message().contains("Unclosed token"));
    }
    
    @Test
    void testMissingBracket() {
        ParseResult result = RegeReader.parse("τhello]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Expected '['"));
    }
    
    @Test
    void testUnclosedParenthesis() {
        ParseResult result = RegeReader.parse("(τ[a]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unclosed parenthesis"));
    }
    
    @Test
    void testUnexpectedClosingParenthesis() {
        ParseResult result = RegeReader.parse("τ[a])");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unexpected trailing"));
    }
    
    @Test
    void testUnexpectedCharacter() {
        ParseResult result = RegeReader.parse("@");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unexpected character"));
    }
    
    @Test
    void testEmptyInput() {
        ParseResult result = RegeReader.parse("");
        
        assertTrue(result.isFailure());
    }
    
    @Test
    void testMissingRightOperandUnion() {
        ParseResult result = RegeReader.parse("τ[a]|");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("after union operator"));
    }
    
    @Test
    void testMissingRightOperandConcat() {
        ParseResult result = RegeReader.parse("τ[a].");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("after concatenation operator"));
    }
    
    @Test
    void testErrorPositionTracking() {
        ParseResult result = RegeReader.parse("τ[a] τ[unclosed");
        
        assertTrue(result.isFailure());
        ParseResult.Failure failure = (ParseResult.Failure) result;
        ParseError error = failure.errors().get(0);
        
        // Error should be somewhere in the input
        assertTrue(error.range().start().offset() >= 0);
        assertTrue(error.range().end().offset() <= "τ[a] τ[unclosed".length());
    }
    
    @Test
    void testFormatErrorsWithSource() {
        ParseResult result = RegeReader.parse("τ[hello");
        
        String formatted = result.formatErrors();
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("τ[hello"));
        assertTrue(formatted.contains("^"));
    }
    
    @Test
    void testToOptionalSuccess() {
        ParseResult result = RegeReader.parse("τ[a]");
        
        assertTrue(result.toOptional().isPresent());
    }
    
    @Test
    void testToOptionalFailure() {
        ParseResult result = RegeReader.parse("τ[unclosed");
        
        assertFalse(result.toOptional().isPresent());
    }
    
    @Test
    void testOrElseSuccess() {
        Expression expr = RegeReader.parse("τ[a]").orElse(Expression.EMPTY);
        
        assertNotEquals(Expression.EMPTY, expr);
    }
    
    @Test
    void testOrElseFailure() {
        Expression expr = RegeReader.parse("τ[unclosed").orElse(Expression.EMPTY);
        
        assertEquals(Expression.EMPTY, expr);
    }
    
    @Test
    void testMapSuccess() {
        ParseResult result = RegeReader.parse("τ[a]");
        ParseResult mapped = result.map(expr -> expr.star());
        
        assertTrue(mapped.isSuccess());
    }
    
    @Test
    void testMapFailure() {
        ParseResult result = RegeReader.parse("τ[unclosed");
        ParseResult mapped = result.map(expr -> expr.star());
        
        assertTrue(mapped.isFailure());
        assertEquals(result, mapped);
    }
    
    @Test
    void testBackwardCompatibilityReadExpression() {
        @SuppressWarnings("deprecation")
        Expression expr = RegeReader.readExpression("τ[a]");
        
        assertNotNull(expr);
    }
    
    @Test
    void testBackwardCompatibilityReadExpressionFailure() {
        @SuppressWarnings("deprecation")
        Expression expr = RegeReader.readExpression("τ[unclosed");
        
        assertNull(expr);
    }
}
