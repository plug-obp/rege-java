# Reader Infrastructure

**Type-safe parsing infrastructure with Result Monad pattern and LSP compatibility**

A standalone, reusable Java library providing comprehensive parsing utilities for building text parsers with precise error reporting and position tracking.

## Overview

This library implements the Result Monad pattern for type-safe error handling in parsers, with full support for:

- ✅ **Type-safe error handling** via sealed interfaces
- ✅ **Position tracking** with line, column, and offset
- ✅ **LSP compatibility** for IDE integration
- ✅ **Functional composition** with map/flatMap
- ✅ **Zero dependencies** - pure Java 23
- ✅ **Generic design** - works with any parsed type

## Quick Start

```java
// 1. Build your parser returning ParseResult<T>
public static ParseResult<Expression> parse(String input) {
    MyParser parser = new MyParser(input);
    Expression result = parser.parseExpression();
    
    if (!parser.errors.isEmpty()) {
        return new ParseResult.Failure<>(parser.errors, input);
    }
    return new ParseResult.Success<>(result);
}

// 2. Handle results with pattern matching
ParseResult<Expression> result = MyParser.parse("x + y");
switch (result) {
    case ParseResult.Success<Expression>(var expr) -> 
        System.out.println("Parsed: " + expr);
    case ParseResult.Failure<Expression>(var errors, var source) -> 
        errors.forEach(e -> System.err.println(e.formatWithSource(source)));
}
```

## Core Components

### ParseResult - Result Monad

Generic sealed interface for type-safe parse results:

```java
sealed interface ParseResult<T> {
    record Success<T>(T value) {}
    record Failure<T>(List<ParseError> errors, String source) {}
}
```

**Usage patterns:**

```java
// Pattern matching (recommended)
switch (result) {
    case ParseResult.Success<MyType>(var value) -> processValue(value);
    case ParseResult.Failure<MyType>(var errors, var source) -> handleErrors(errors);
}

// Functional composition
ParseResult<ProcessedType> processed = parser.parse(input)
    .map(v -> transform(v))
    .flatMap(v -> validate(v))
    .map(v -> finalize(v));

// Exception-based (for legacy code)
try {
    MyType value = parser.parse(input).orElseThrow();
} catch (ParseException e) {
    logger.error("Parse failed: {}", e.getMessage());
}

// Optional-based
Optional<MyType> opt = parser.parse(input).toOptional();
```

### Position - Source Location

Tracks positions in source text with three coordinates:

```java
public record Position(
    int line,      // 1-based (human-readable)
    int column,    // 1-based (human-readable)
    int offset     // 0-based (programmatic)
) implements Comparable<Position> {
    public static Position start() { return new Position(1, 1, 0); }
}
```

**Example:**
```java
Position pos = new Position(1, 5, 4);  // Line 1, col 5, offset 4
Position start = Position.start();      // (1, 1, 0)
```

### Range - Text Spans

Represents ranges from start to end position:

```java
public record Range(Position start, Position end) {
    public static Range at(Position pos) { /* zero-width range */ }
    public boolean contains(Position pos) { /* ... */ }
    public int length() { /* ... */ }
}
```

**Example:**
```java
Range span = new Range(start, end);
Range point = Range.at(position);  // Zero-width

if (span.contains(somePosition)) {
    int chars = span.length();
}
```

### ParseError - Error Reporting

Detailed error information with LSP compatibility:

```java
public record ParseError(
    Range range,
    String message,
    Severity severity,      // ERROR, WARNING, INFO, HINT
    Optional<String> code
) {
    public String formatWithSource(String source) { /* ... */ }
}
```

**Example:**
```java
ParseError error = new ParseError(
    range,
    "Unexpected character '}'",
    ParseError.Severity.ERROR,
    Optional.of("unexpected-char")
);

String formatted = error.formatWithSource(source);
// Output:
// error at 1:5-1:6: Unexpected character '}'
//   let x = }
//           ^
```

**Severity levels** (LSP DiagnosticSeverity):
- `ERROR` (1) - Prevents parsing
- `WARNING` (2) - Doesn't prevent parsing
- `INFO` (3) - Informational
- `HINT` (4) - Suggestion

### ParseException - Exception Bridge

Bridges Result Monad and exception-based error handling:

```java
try {
    T result = parseResult.orElseThrow();
} catch (ParseException e) {
    List<ParseError> errors = e.getErrors();
    String source = e.getSource();
    String message = e.getMessage();  // Formatted multi-error
}
```

### Peekable - Character Iterator

Character-by-character parsing with lookahead and position tracking:

```java
Peekable input = new Peekable("hello world");
Position start = input.position();

if (input.hasNext() && input.peek() == 'h') {
    char ch = input.next();  // Consume 'h'
    Range range = input.rangeFrom(start);
}
```

**Features:**
- `peek()` - Look ahead without consuming
- `next()` - Consume and return character
- `position()` - Current position
- `rangeFrom(start)` - Create range from start to current
- `rangeHere()` - Zero-width range at current position
- `source()` - Get original source string

**Position tracking:**
- Automatically increments offset
- Handles newlines (increments line, resets column)
- Always accurate for error reporting

### AlienValidator - Embedded Syntax Validation

Validates content with "alien syntax" - syntax unknown to the host parser. This enables compositional parsing where one parser delegates validation of embedded content to another validator:

```java
// Accept everything (default)
AlienValidator permissive = AlienValidator.acceptAll();

// Reject empty content
AlienValidator noEmpty = AlienValidator.nonEmpty();

// Pattern-based validation
AlienValidator lowercase = AlienValidator.pattern("[a-z]+", "Lowercase letters only");

// Custom validation logic
AlienValidator jsonValidator = (content, range) -> {
    try {
        parseJson(content);
        return new ParseResult.Success<>(content);
    } catch (Exception e) {
        return new ParseResult.Failure<>(
            List.of(new ParseError(range, "Invalid JSON: " + e.getMessage())),
            content
        );
    }
};
```

**Composition:**

```java
// Both validators must succeed
AlienValidator strict = AlienValidator.nonEmpty()
    .and(AlienValidator.pattern("[a-zA-Z0-9]+", "Alphanumeric only"))
    .and(customValidator);

// Either validator can succeed
AlienValidator lenient = jsonValidator.or(xmlValidator);
```

**Common use cases:**
- Validating embedded JSON/XML in configuration files
- Checking mathematical expressions in template literals
- Verifying SQL queries in string constants
- Any domain-specific language embedded within another

**Integration with parsers:**

```java
public static ParseResult<Token> parseToken(String input, AlienValidator validator) {
    // ... parse token structure ...
    
    // Validate token content
    Range contentRange = new Range(contentStart, contentEnd);
    ParseResult<String> validationResult = validator.validate(tokenValue, contentRange);
    
    if (validationResult instanceof ParseResult.Failure<String> failure) {
        errors.addAll(failure.errors());  // Collect validation errors
        return null;
    }
    
    return new ParseResult.Success<>(new Token(tokenValue));
}
```

## Building a Parser

Complete example of a simple parser:

```java
public class MyParser {
    private final List<ParseError> errors = new ArrayList<>();
    private final Peekable input;
    
    public static ParseResult<Expression> parse(String source) {
        MyParser parser = new MyParser(source);
        Expression result = parser.parseExpression();
        
        if (!parser.errors.isEmpty()) {
            return new ParseResult.Failure<>(parser.errors, source);
        }
        if (result == null) {
            parser.error(parser.input.rangeHere(), "Failed to parse expression");
            return new ParseResult.Failure<>(parser.errors, source);
        }
        return new ParseResult.Success<>(result);
    }
    
    private MyParser(String source) {
        this.input = new Peekable(source);
    }
    
    private void error(Range range, String message) {
        errors.add(new ParseError(range, message));
    }
    
    private Expression parseExpression() {
        Position start = input.position();
        
        if (!input.hasNext()) {
            error(input.rangeHere(), "Unexpected end of input");
            return null;
        }
        
        if (input.peek() == '(') {
            input.next();  // consume '('
            Expression inner = parseExpression();
            
            if (!input.hasNext() || input.peek() != ')') {
                error(input.rangeFrom(start), "Unclosed parenthesis");
                return null;
            }
            input.next();  // consume ')'
            return inner;
        }
        
        // More parsing logic...
        
        return result;
    }
}
```

## LSP Integration

This infrastructure maps directly to Language Server Protocol types:

```java
import org.eclipse.lsp4j.*;

// Convert ParseError to LSP Diagnostic
public Diagnostic toDiagnostic(ParseError error) {
    return new Diagnostic(
        toLspRange(error.range()),
        error.message(),
        DiagnosticSeverity.forValue(error.severity().getLspValue()),
        "my-parser",
        error.code().orElse(null)
    );
}

// Convert Range to LSP Range
private org.eclipse.lsp4j.Range toLspRange(Range range) {
    return new org.eclipse.lsp4j.Range(
        new org.eclipse.lsp4j.Position(
            range.start().line() - 1,    // LSP is 0-based
            range.start().column() - 1
        ),
        new org.eclipse.lsp4j.Position(
            range.end().line() - 1,
            range.end().column() - 1
        )
    );
}

// Publish diagnostics
public void publishDiagnostics(String uri, ParseResult<?> result) {
    if (result instanceof ParseResult.Failure<?>failure) {
        List<Diagnostic> diagnostics = failure.errors().stream()
            .map(this::toDiagnostic)
            .toList();
        
        languageClient.publishDiagnostics(
            new PublishDiagnosticsParams(uri, diagnostics)
        );
    }
}
```

## Design Principles

- **Type Safety** - Sealed interfaces eliminate null checks
- **Immutability** - All structures are immutable records
- **Composability** - Functional methods enable clean chaining
- **LSP Compatible** - Direct mapping to LSP types
- **Zero Dependencies** - Pure Java 23 standard library
- **Generic** - Works with any parsed value type
- **Performance** - Minimal overhead, efficient data structures

## Performance

- **Peekable**: O(1) character access via direct string indexing
- **Position tracking**: Minimal per-character overhead
- **Error collection**: Amortized O(1) append with ArrayList
- **Immutable collections**: Defensive copies prevent mutations
- **Records**: Efficient memory layout and fast equality checks

## Requirements

- Java 23 or higher (for sealed interfaces and records)
- No external dependencies

## Test Coverage

125 comprehensive tests covering:
- Position validation and comparison
- Range creation and validation
- ParseError formatting and severity
- ParseResult pattern matching and composition
- ParseException formatting
- Peekable position tracking and newline handling

## License

Part of the rege-language project.

## See Also

- [rege-core](../rege-core/README.md) - Regular expression parser using this infrastructure
- [package-info.java](src/main/java/rege/reader/infra/package-info.java) - Detailed API documentation
