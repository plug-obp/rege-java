package rege.syntax;
import rege.reader.infra.*;

import org.junit.jupiter.api.Test;
import rege.syntax.model.Expression;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegeReaderLeft error reporting with ParseResult.
 */
class RegeReaderLeftErrorTest {
    
    @Test
    void testSuccessfulParse() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]");
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
    
    @Test
    void testSuccessfulParsePattern() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]|τ[b]");
        
        switch (result) {
            case ParseResult.Success(var expr) -> assertNotNull(expr);
            case ParseResult.Failure(var errors, var source) -> 
                fail("Expected success but got " + errors.size() + " errors");
        }
    }
    
    @Test
    void testOrElseThrow() throws ParseException {
        Expression expr = RegeReaderLeft.parse("τ[a]").orElseThrow();
        assertNotNull(expr);
    }
    
    @Test
    void testOrElseThrowFailure() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[unclosed");
        
        ParseException ex = assertThrows(ParseException.class, result::orElseThrow);
        assertFalse(ex.getErrors().isEmpty());
    }
    
    @Test
    void testUnclosedToken() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[hello");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertEquals(1, failure.errors().size());
        assertTrue(failure.errors().get(0).message().contains("Unclosed token"));
    }
    
    @Test
    void testMissingBracket() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τhello]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Expected '['"));
    }
    
    @Test
    void testUnclosedParenthesis() {
        ParseResult<Expression> result = RegeReaderLeft.parse("(τ[a]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unclosed parenthesis"));
    }
    
    @Test
    void testUnexpectedClosingParenthesis() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a])");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unexpected trailing"));
    }
    
    @Test
    void testUnexpectedCharacter() {
        ParseResult<Expression> result = RegeReaderLeft.parse("@");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("Unexpected character"));
    }
    
    @Test
    void testEmptyInput() {
        ParseResult<Expression> result = RegeReaderLeft.parse("");
        
        assertTrue(result.isFailure());
    }
    
    @Test
    void testMissingRightOperandUnion() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]|");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("after union operator"));
    }
    
    @Test
    void testMissingRightOperandConcat() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a].");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().get(0).message().contains("after concatenation operator"));
    }
    
    @Test
    void testErrorPositionTracking() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a] τ[unclosed");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        ParseError error = failure.errors().get(0);
        
        // Error should be somewhere in the input
        assertTrue(error.range().start().offset() >= 0);
        assertTrue(error.range().end().offset() <= "τ[a] τ[unclosed".length());
    }
    
    @Test
    void testFormatErrorsWithSource() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[hello");
        
        String formatted = result.formatErrors();
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("τ[hello"));
        assertTrue(formatted.contains("^"));
    }
    
    @Test
    void testToOptionalSuccess() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]");
        
        assertTrue(result.toOptional().isPresent());
    }
    
    @Test
    void testToOptionalFailure() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[unclosed");
        
        assertFalse(result.toOptional().isPresent());
    }
    
    @Test
    void testOrElseSuccess() {
        Expression expr = RegeReaderLeft.parse("τ[a]").orElse(Expression.EMPTY);
        
        assertNotEquals(Expression.EMPTY, expr);
    }
    
    @Test
    void testOrElseFailure() {
        Expression expr = RegeReaderLeft.parse("τ[unclosed").orElse(Expression.EMPTY);
        
        assertEquals(Expression.EMPTY, expr);
    }
    
    @Test
    void testMapSuccess() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]");
        ParseResult mapped = result.map(expr -> expr.star());
        
        assertTrue(mapped.isSuccess());
    }
    
    @Test
    void testMapFailure() {
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[unclosed");
        ParseResult mapped = result.map(expr -> expr.star());
        
        assertTrue(mapped.isFailure());
        assertEquals(result, mapped);
    }
    
    @Test
    void testBackwardCompatibilityReadExpression() {
        @SuppressWarnings("deprecation")
        Expression expr = RegeReaderLeft.readExpression("τ[a]");
        
        assertNotNull(expr);
    }
    
    @Test
    void testBackwardCompatibilityReadExpressionFailure() {
        @SuppressWarnings("deprecation")
        Expression expr = RegeReaderLeft.readExpression("τ[unclosed");
        
        assertNull(expr);
    }
    
    @Test
    void testLeftAssociativityPreserved() {
        // Verify left-associativity is maintained with new API (using isSmart=false)
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[a]|τ[b]|τ[c]", false);
        
        assertTrue(result.isSuccess());
        Expression expr = ((ParseResult.Success<Expression>) result).value();
        
        // Should be ((a|b)|c) - left associative
        assertTrue(expr instanceof rege.syntax.model.Union);
        rege.syntax.model.Union outerUnion = (rege.syntax.model.Union) expr;
        assertTrue(outerUnion.lhs() instanceof rege.syntax.model.Union);
    }
    
    // ============================================================================
    // Alien Validator Tests
    // ============================================================================
    
    @Test
    void testAlienValidatorAcceptAll() {
        AlienValidator validator = AlienValidator.acceptAll();
        ParseResult<Expression> result = RegeReaderLeft.parse("τ[anything!@#$]", true, validator);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testAlienValidatorPattern() {
        AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase letters only");
        
        // Valid: lowercase
        ParseResult<Expression> result1 = RegeReaderLeft.parse("τ[hello]", true, lowercase);
        assertTrue(result1.isSuccess());
        
        // Invalid: contains uppercase
        ParseResult<Expression> result2 = RegeReaderLeft.parse("τ[Hello]", true, lowercase);
        assertTrue(result2.isFailure());
        
        if (result2 instanceof ParseResult.Failure<Expression> failure) {
            assertEquals(1, failure.errors().size());
            assertEquals("Lowercase letters only", failure.errors().get(0).message());
        }
    }
    
    @Test
    void testAlienValidatorMultipleTokens() {
        AlienValidator alphanumeric = AlienValidator.pattern("[a-zA-Z0-9]+", "Alphanumeric only");
        
        // All valid tokens
        ParseResult<Expression> result1 = RegeReaderLeft.parse("τ[hello]|τ[world123]", true, alphanumeric);
        assertTrue(result1.isSuccess());
        
        // One invalid token
        ParseResult<Expression> result2 = RegeReaderLeft.parse("τ[hello]|τ[world!]", true, alphanumeric);
        assertTrue(result2.isFailure());
        
        if (result2 instanceof ParseResult.Failure<Expression> failure) {
            // Should have at least one validation error
            assertTrue(failure.errors().size() >= 1);
            boolean hasAlphanumericError = failure.errors().stream()
                .anyMatch(e -> e.message().contains("Alphanumeric"));
            assertTrue(hasAlphanumericError, "Should have alphanumeric validation error");
        }
    }
    
    @Test
    void testAlienValidatorComposition() {
        AlienValidator strict = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"));
        
        // Valid
        ParseResult<Expression> result1 = RegeReaderLeft.parse("τ[hello]", true, strict);
        assertTrue(result1.isSuccess());
        
        // Invalid: not lowercase
        ParseResult<Expression> result2 = RegeReaderLeft.parse("τ[HELLO]", true, strict);
        assertTrue(result2.isFailure());
    }
    
    @Test
    void testAlienValidatorWithLeftAssociativity() {
        AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase only");
        
        // Valid complex expression (left-associative)
        ParseResult<Expression> result1 = RegeReaderLeft.parse(
            "τ[hello]|τ[world]|τ[test]",
            true,
            lowercase
        );
        assertTrue(result1.isSuccess());
        
        // Invalid: middle token fails validation
        ParseResult<Expression> result2 = RegeReaderLeft.parse(
            "τ[hello]|τ[WORLD]|τ[test]",
            true,
            lowercase
        );
        assertTrue(result2.isFailure());
    }
}
