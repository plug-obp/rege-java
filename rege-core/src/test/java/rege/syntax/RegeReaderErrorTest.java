package rege.syntax;
import rege.reader.infra.*;

import org.junit.jupiter.api.Test;
import rege.syntax.model.Expression;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegeReader error reporting with ParseResult.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class RegeReaderErrorTest {
    
    @Test
    void testSuccessfulParse() {
        ParseResult<Expression> result = RegeReader.parse("τ[a]");
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
    
    @Test
    void testSuccessfulParsePattern() {
        ParseResult<Expression> result = RegeReader.parse("τ[a]|τ[b]");
        
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
        ParseResult<Expression> result = RegeReader.parse("τ[unclosed");
        
        ParseException ex = assertThrows(ParseException.class, result::orElseThrow);
        assertFalse(ex.getErrors().isEmpty());
    }
    
    @Test
    void testUnclosedToken() {
        ParseResult<Expression> result = RegeReader.parse("τ[hello");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertEquals(1, failure.errors().size());
        assertTrue(failure.errors().getFirst().message().contains("Unclosed token"));
    }
    
    @Test
    void testMissingBracket() {
        ParseResult<Expression> result = RegeReader.parse("τhello]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("Expected '['"));
    }
    
    @Test
    void testUnclosedParenthesis() {
        ParseResult<Expression> result = RegeReader.parse("(τ[a]");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("Unclosed parenthesis"));
    }
    
    @Test
    void testUnexpectedClosingParenthesis() {
        ParseResult<Expression> result = RegeReader.parse("τ[a])");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("Unexpected trailing"));
    }
    
    @Test
    void testUnexpectedCharacter() {
        ParseResult<Expression> result = RegeReader.parse("@");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("Unexpected character"));
    }
    
    @Test
    void testEmptyInput() {
        ParseResult<Expression> result = RegeReader.parse("");
        
        assertTrue(result.isFailure());
    }
    
    @Test
    void testMissingRightOperandUnion() {
        ParseResult<Expression> result = RegeReader.parse("τ[a]|");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("after union operator"));
    }
    
    @Test
    void testMissingRightOperandConcat() {
        ParseResult<Expression> result = RegeReader.parse("τ[a].");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        assertTrue(failure.errors().getFirst().message().contains("after concatenation operator"));
    }
    
    @Test
    void testErrorPositionTracking() {
        ParseResult<Expression> result = RegeReader.parse("τ[a] τ[unclosed");
        
        assertTrue(result.isFailure());
        ParseResult.Failure<Expression> failure = (ParseResult.Failure) result;
        ParseError error = failure.errors().getFirst();
        
        // Error should be somewhere in the input
        assertTrue(error.range().start().offset() >= 0);
        assertTrue(error.range().end().offset() <= "τ[a] τ[unclosed".length());
    }
    
    @Test
    void testFormatErrorsWithSource() {
        ParseResult<Expression> result = RegeReader.parse("τ[hello");
        
        String formatted = result.formatErrors();
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("τ[hello"));
        assertTrue(formatted.contains("^"));
    }
    
    @Test
    void testToOptionalSuccess() {
        ParseResult<Expression> result = RegeReader.parse("τ[a]");
        
        assertTrue(result.toOptional().isPresent());
    }
    
    @Test
    void testToOptionalFailure() {
        ParseResult<Expression> result = RegeReader.parse("τ[unclosed");
        
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
        ParseResult<Expression> result = RegeReader.parse("τ[a]");
        ParseResult mapped = result.map(Expression::star);
        
        assertTrue(mapped.isSuccess());
    }
    
    @Test
    void testMapFailure() {
        ParseResult<Expression> result = RegeReader.parse("τ[unclosed");
        ParseResult mapped = result.map(Expression::star);
        
        assertTrue(mapped.isFailure());
        assertEquals(result, mapped);
    }
    
    // ============================================================================
    // Alien Validator Tests
    // ============================================================================
    
    @Test
    void testAlienValidatorAcceptAll() {
        AlienValidator validator = AlienValidator.acceptAll();
        ParseResult<Expression> result = RegeReader.parse("τ[anything!@#$]", true, validator);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testAlienValidatorNonEmpty() {
        AlienValidator validator = AlienValidator.nonEmpty();
        
        // Non-empty should succeed
        ParseResult<Expression> result1 = RegeReader.parse("τ[hello]", true, validator);
        assertTrue(result1.isSuccess());
        
        // Empty should fail (but parser converts to epsilon, so this tests validation is not called)
        ParseResult<Expression> result2 = RegeReader.parse("τ[]", true, validator);
        assertTrue(result2.isSuccess()); // Empty tokens become epsilon before validation
    }
    
    @Test
    void testAlienValidatorPattern() {
        AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase letters only");
        
        // Valid: lowercase
        ParseResult<Expression> result1 = RegeReader.parse("τ[hello]", true, lowercase);
        assertTrue(result1.isSuccess());
        
        // Invalid: contains uppercase
        ParseResult<Expression> result2 = RegeReader.parse("τ[Hello]", true, lowercase);
        assertTrue(result2.isFailure());
        
        if (result2 instanceof ParseResult.Failure<Expression> failure) {
            assertEquals(1, failure.errors().size());
            assertEquals("Lowercase letters only", failure.errors().getFirst().message());
        }
    }
    
    @Test
    void testAlienValidatorMultipleTokens() {
        AlienValidator alphanumeric = AlienValidator.pattern("[a-zA-Z0-9]+", "Alphanumeric only");
        
        // All valid tokens
        ParseResult<Expression> result1 = RegeReader.parse("τ[hello]|τ[world123]", true, alphanumeric);
        assertTrue(result1.isSuccess());
        
        // One invalid token
        ParseResult<Expression> result2 = RegeReader.parse("τ[hello]|τ[world!]", true, alphanumeric);
        assertTrue(result2.isFailure());
        
        if (result2 instanceof ParseResult.Failure<Expression> failure) {
            // Should have at least one validation error
            assertFalse(failure.errors().isEmpty());
            boolean hasAlphanumericError = failure.errors().stream()
                .anyMatch(e -> e.message().contains("Alphanumeric"));
            assertTrue(hasAlphanumericError, "Should have alphanumeric validation error");
        }
    }
    
    @Test
    void testAlienValidatorComposition() {
        AlienValidator strict = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"))
            .and(AlienValidator.pattern(".{3,}", "At least 3 characters"));
        
        // Valid
        ParseResult<Expression> result1 = RegeReader.parse("τ[hello]", true, strict);
        assertTrue(result1.isSuccess());
        
        // Invalid: too short
        ParseResult<Expression> result2 = RegeReader.parse("τ[ab]", true, strict);
        assertTrue(result2.isFailure());
        
        // Invalid: not lowercase
        ParseResult<Expression> result3 = RegeReader.parse("τ[HELLO]", true, strict);
        assertTrue(result3.isFailure());
    }
    
    @Test
    void testAlienValidatorErrorPosition() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase only");
        
        ParseResult<Expression> result = RegeReader.parse("τ[HELLO]", true, validator);
        
        assertTrue(result.isFailure());
        if (result instanceof ParseResult.Failure<Expression> failure) {
            ParseError error = failure.errors().getFirst();
            
            // Error should point to the token value (inside brackets)
            assertTrue(error.range().start().column() > 1); // After 'τ['
            assertNotNull(error.message());
        }
    }
    
    @Test
    void testAlienValidatorWithComplexExpression() {
        AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase only");
        
        // Valid complex expression
        ParseResult<Expression> result1 = RegeReader.parse(
            "(τ[hello]|τ[world])*",
            true,
            lowercase
        );
        assertTrue(result1.isSuccess());
        
        // Invalid: one token fails validation
        ParseResult<Expression> result2 = RegeReader.parse(
            "(τ[hello]|τ[WORLD])*",
            true,
            lowercase
        );
        assertTrue(result2.isFailure());
    }
    
    @Test
    void testAlienValidatorCustom() {
        // Custom validator: balanced parentheses
        AlienValidator balanced = (content, range) -> {
            int depth = 0;
            for (char c : content.toCharArray()) {
                if (c == '(') depth++;
                if (c == ')') depth--;
                if (depth < 0) {
                    return new ParseResult.Failure<>(
                        java.util.List.of(new ParseError(range, "Unbalanced parentheses")),
                        content
                    );
                }
            }
            if (depth != 0) {
                return new ParseResult.Failure<>(
                    java.util.List.of(new ParseError(range, "Unbalanced parentheses")),
                    content
                );
            }
            return new ParseResult.Success<>(content);
        };
        
        // Valid
        ParseResult<Expression> result1 = RegeReader.parse("τ[(a(b)c)]", true, balanced);
        assertTrue(result1.isSuccess());
        
        // Invalid
        ParseResult<Expression> result2 = RegeReader.parse("τ[(a(b)]", true, balanced);
        assertTrue(result2.isFailure());
    }
}
