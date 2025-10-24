package rege.reader.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlienValidatorTest {
    
    private static final Position START = new Position(1, 1, 0);
    private static final Position END = new Position(1, 6, 5);
    private static final Range RANGE = new Range(START, END);
    
    // ============================================================================
    // acceptAll() Tests
    // ============================================================================
    
    @Test
    @DisplayName("acceptAll() should accept any content")
    void acceptAllAcceptsAnything() {
        AlienValidator validator = AlienValidator.acceptAll();
        
        ParseResult<String> result = validator.validate("hello", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("hello", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("acceptAll() should accept empty content")
    void acceptAllAcceptsEmpty() {
        AlienValidator validator = AlienValidator.acceptAll();
        
        ParseResult<String> result = validator.validate("", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("acceptAll() should accept special characters")
    void acceptAllAcceptsSpecialChars() {
        AlienValidator validator = AlienValidator.acceptAll();
        
        ParseResult<String> result = validator.validate("!@#$%^&*()", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("!@#$%^&*()", ((ParseResult.Success<String>) result).value());
    }
    
    // ============================================================================
    // nonEmpty() Tests
    // ============================================================================
    
    @Test
    @DisplayName("nonEmpty() should accept non-empty content")
    void nonEmptyAcceptsContent() {
        AlienValidator validator = AlienValidator.nonEmpty();
        
        ParseResult<String> result = validator.validate("hello", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("hello", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("nonEmpty() should reject empty content")
    void nonEmptyRejectsEmpty() {
        AlienValidator validator = AlienValidator.nonEmpty();
        
        ParseResult<String> result = validator.validate("", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals(1, failure.errors().size());
        assertEquals("Content cannot be empty", failure.errors().get(0).message());
        assertEquals(RANGE, failure.errors().get(0).range());
        assertEquals(ParseError.Severity.ERROR, failure.errors().get(0).severity());
    }
    
    @Test
    @DisplayName("nonEmpty() error should have error code")
    void nonEmptyErrorHasCode() {
        AlienValidator validator = AlienValidator.nonEmpty();
        
        ParseResult<String> result = validator.validate("", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertTrue(failure.errors().get(0).code().isPresent());
        assertEquals("empty-content", failure.errors().get(0).code().get());
    }
    
    // ============================================================================
    // pattern() Tests
    // ============================================================================
    
    @Test
    @DisplayName("pattern() should accept matching content")
    void patternAcceptsMatch() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase letters only");
        
        ParseResult<String> result = validator.validate("hello", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("hello", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("pattern() should reject non-matching content")
    void patternRejectsNonMatch() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase letters only");
        
        ParseResult<String> result = validator.validate("Hello123", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals(1, failure.errors().size());
        assertEquals("Lowercase letters only", failure.errors().get(0).message());
        assertEquals(RANGE, failure.errors().get(0).range());
    }
    
    @Test
    @DisplayName("pattern() error should have error code")
    void patternErrorHasCode() {
        AlienValidator validator = AlienValidator.pattern("[0-9]+", "Numbers only");
        
        ParseResult<String> result = validator.validate("abc", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertTrue(failure.errors().get(0).code().isPresent());
        assertEquals("pattern-mismatch", failure.errors().get(0).code().get());
    }
    
    @Test
    @DisplayName("pattern() should work with complex regex")
    void patternComplexRegex() {
        AlienValidator validator = AlienValidator.pattern(
            "[a-zA-Z_][a-zA-Z0-9_]*",
            "Must be valid identifier"
        );
        
        // Valid identifiers
        assertInstanceOf(ParseResult.Success.class, validator.validate("foo", RANGE));
        assertInstanceOf(ParseResult.Success.class, validator.validate("_bar", RANGE));
        assertInstanceOf(ParseResult.Success.class, validator.validate("foo123", RANGE));
        
        // Invalid identifiers
        assertInstanceOf(ParseResult.Failure.class, validator.validate("123foo", RANGE));
        assertInstanceOf(ParseResult.Failure.class, validator.validate("foo-bar", RANGE));
    }
    
    // ============================================================================
    // and() Composition Tests
    // ============================================================================
    
    @Test
    @DisplayName("and() should succeed when both validators pass")
    void andSucceedsWhenBothPass() {
        AlienValidator validator = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"));
        
        ParseResult<String> result = validator.validate("hello", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("hello", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("and() should fail when first validator fails")
    void andFailsWhenFirstFails() {
        AlienValidator validator = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"));
        
        ParseResult<String> result = validator.validate("", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals("Content cannot be empty", failure.errors().get(0).message());
    }
    
    @Test
    @DisplayName("and() should fail when second validator fails")
    void andFailsWhenSecondFails() {
        AlienValidator validator = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"));
        
        ParseResult<String> result = validator.validate("HELLO", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals("Lowercase only", failure.errors().get(0).message());
    }
    
    @Test
    @DisplayName("and() should short-circuit on first failure")
    void andShortCircuits() {
        boolean[] secondCalled = {false};
        
        AlienValidator first = (content, range) -> new ParseResult.Failure<>(
            List.of(new ParseError(range, "First failed")),
            content
        );
        
        AlienValidator second = (content, range) -> {
            secondCalled[0] = true;
            return new ParseResult.Success<>(content);
        };
        
        AlienValidator combined = first.and(second);
        combined.validate("test", RANGE);
        
        assertFalse(secondCalled[0], "Second validator should not be called");
    }
    
    @Test
    @DisplayName("and() should chain multiple validators")
    void andChainsMultiple() {
        AlienValidator validator = AlienValidator.nonEmpty()
            .and(AlienValidator.pattern("[a-z]+", "Lowercase only"))
            .and(AlienValidator.pattern(".{3,}", "At least 3 characters"));
        
        assertInstanceOf(ParseResult.Success.class, validator.validate("hello", RANGE));
        assertInstanceOf(ParseResult.Failure.class, validator.validate("ab", RANGE));
        assertInstanceOf(ParseResult.Failure.class, validator.validate("ABC", RANGE));
        assertInstanceOf(ParseResult.Failure.class, validator.validate("", RANGE));
    }
    
    // ============================================================================
    // or() Composition Tests
    // ============================================================================
    
    @Test
    @DisplayName("or() should succeed when first validator passes")
    void orSucceedsWhenFirstPasses() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase")
            .or(AlienValidator.pattern("[A-Z]+", "Uppercase"));
        
        ParseResult<String> result = validator.validate("hello", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("hello", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("or() should succeed when second validator passes")
    void orSucceedsWhenSecondPasses() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase")
            .or(AlienValidator.pattern("[A-Z]+", "Uppercase"));
        
        ParseResult<String> result = validator.validate("HELLO", RANGE);
        
        assertInstanceOf(ParseResult.Success.class, result);
        assertEquals("HELLO", ((ParseResult.Success<String>) result).value());
    }
    
    @Test
    @DisplayName("or() should fail when both validators fail")
    void orFailsWhenBothFail() {
        AlienValidator validator = AlienValidator.pattern("[a-z]+", "Lowercase")
            .or(AlienValidator.pattern("[A-Z]+", "Uppercase"));
        
        ParseResult<String> result = validator.validate("123", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals(2, failure.errors().size(), "Should combine errors from both validators");
    }
    
    @Test
    @DisplayName("or() should short-circuit on first success")
    void orShortCircuits() {
        boolean[] secondCalled = {false};
        
        AlienValidator first = (content, range) -> new ParseResult.Success<>(content);
        
        AlienValidator second = (content, range) -> {
            secondCalled[0] = true;
            return new ParseResult.Success<>(content);
        };
        
        AlienValidator combined = first.or(second);
        combined.validate("test", RANGE);
        
        assertFalse(secondCalled[0], "Second validator should not be called");
    }
    
    @Test
    @DisplayName("or() should combine errors from both validators")
    void orCombinesErrors() {
        AlienValidator v1 = (content, range) -> new ParseResult.Failure<>(
            List.of(new ParseError(range, "Error 1")),
            content
        );
        
        AlienValidator v2 = (content, range) -> new ParseResult.Failure<>(
            List.of(new ParseError(range, "Error 2")),
            content
        );
        
        AlienValidator combined = v1.or(v2);
        ParseResult<String> result = combined.validate("test", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals(2, failure.errors().size());
        assertEquals("Error 1", failure.errors().get(0).message());
        assertEquals("Error 2", failure.errors().get(1).message());
    }
    
    // ============================================================================
    // Custom Validator Tests
    // ============================================================================
    
    @Test
    @DisplayName("Custom validator can implement complex logic")
    void customValidator() {
        // Validator that checks for balanced parentheses
        AlienValidator balanced = (content, range) -> {
            int depth = 0;
            for (char c : content.toCharArray()) {
                if (c == '(') depth++;
                if (c == ')') depth--;
                if (depth < 0) {
                    return new ParseResult.Failure<>(
                        List.of(new ParseError(range, "Unbalanced parentheses")),
                        content
                    );
                }
            }
            if (depth != 0) {
                return new ParseResult.Failure<>(
                    List.of(new ParseError(range, "Unbalanced parentheses")),
                    content
                );
            }
            return new ParseResult.Success<>(content);
        };
        
        assertInstanceOf(ParseResult.Success.class, balanced.validate("(())", RANGE));
        assertInstanceOf(ParseResult.Success.class, balanced.validate("(a(b)c)", RANGE));
        assertInstanceOf(ParseResult.Failure.class, balanced.validate("(()", RANGE));
        assertInstanceOf(ParseResult.Failure.class, balanced.validate("())", RANGE));
    }
    
    @Test
    @DisplayName("Custom validator can create sub-ranges for precise errors")
    void customValidatorWithSubRanges() {
        AlienValidator validator = (content, range) -> {
            // Find position of first invalid character
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (!Character.isLetterOrDigit(c)) {
                    // Create sub-range for the specific character
                    Position errorPos = new Position(
                        range.start().line(),
                        range.start().column() + i,
                        range.start().offset() + i
                    );
                    Range errorRange = Range.at(errorPos);
                    
                    return new ParseResult.Failure<>(
                        List.of(new ParseError(
                            errorRange,
                            "Invalid character: '" + c + "'"
                        )),
                        content
                    );
                }
            }
            return new ParseResult.Success<>(content);
        };
        
        ParseResult<String> result = validator.validate("abc@def", RANGE);
        
        assertInstanceOf(ParseResult.Failure.class, result);
        ParseResult.Failure<String> failure = (ParseResult.Failure<String>) result;
        assertEquals("Invalid character: '@'", failure.errors().get(0).message());
        
        // Error should point to position of '@' (offset 3 from start)
        Position errorPos = failure.errors().get(0).range().start();
        assertEquals(START.offset() + 3, errorPos.offset());
    }
    
    // ============================================================================
    // Complex Composition Tests
    // ============================================================================
    
    @Test
    @DisplayName("Complex composition: (A and B) or (C and D)")
    void complexComposition() {
        AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase");
        AlienValidator short_ = AlienValidator.pattern(".{1,5}", "Max 5 chars");
        AlienValidator uppercase = AlienValidator.pattern("[A-Z]+", "Uppercase");
        AlienValidator long_ = AlienValidator.pattern(".{6,}", "Min 6 chars");
        
        // Accept either (lowercase AND short) OR (uppercase AND long)
        AlienValidator complex = lowercase.and(short_).or(uppercase.and(long_));
        
        // Valid: lowercase and short
        assertInstanceOf(ParseResult.Success.class, complex.validate("hello", RANGE));
        
        // Valid: uppercase and long
        assertInstanceOf(ParseResult.Success.class, complex.validate("WONDERFUL", RANGE));
        
        // Invalid: lowercase but too long
        assertInstanceOf(ParseResult.Failure.class, complex.validate("wonderful", RANGE));
        
        // Invalid: uppercase but too short
        assertInstanceOf(ParseResult.Failure.class, complex.validate("HI", RANGE));
        
        // Invalid: neither pattern
        assertInstanceOf(ParseResult.Failure.class, complex.validate("123", RANGE));
    }
}
