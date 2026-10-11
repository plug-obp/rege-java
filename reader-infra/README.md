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

All coordinates are zero-based. Columns and absolute offsets count UTF-16 code
units, matching Java strings and LSP's default `utf-16` encoding. Ranges are
half-open: the start is included and the end is excluded. Tabs occupy one code
unit; a supplementary Unicode character occupies two.

This follows the LSP [Position](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#position)
and [Range](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#range)
contracts. An LSP connection negotiating another encoding must convert at its
boundary. A user interface may display one-based numbers, but must not feed them
back into these coordinates.

```java
public record Position(
    int line,      // zero-based (LSP)
    int column,    // zero-based UTF-16 column
    int offset     // zero-based UTF-16 offset
) implements Comparable<Position> {
    public static Position start() { return new Position(0, 0, 0); }
}
```

**Example:**
```java
Position pos = new Position(0, 4, 4);  // Line 0, col 4, offset 4
Position start = Position.start();      // (0, 0, 0)
```

### SourceText - Shared Coordinate Index

`SourceText` owns text indexing independently of any parser or syntax tree. It
recognizes LF, CRLF and CR without changing the original text. EOF is an ordinary
position; a trailing newline creates an empty final line. Inside a CRLF pair,
the column stays at the preceding line's end until the entire pair is consumed;
the original absolute offset is retained.

```java
SourceText text = new SourceText("a😀\r\nb");
Position afterEmoji = text.positionAt(3);  // (line=0, column=3, offset=3)
Range multiline = text.range(1, 6);        // [0:1, 1:1)
int utf16 = text.offsetAtCodePoint(2);      // 3; convert ANTLR code-point indices
int offset = text.offsetAt(1, 1);          // 6; convert an LSP position
```

`positionAt`, `range`, and `offsetAtCodePoint` require in-bounds offsets, including
EOF. `offsetAt(line, column)` clamps excessive columns to the line's text end and
excessive lines to EOF, and rejects negative values. `lineCount`,
`lineStartOffset`, and `lineEndOffset` support line-based tooling; the end excludes
newline characters. A parser must handle synthetic/recovery token indices before
conversion rather than treating a sentinel as a real source location.

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

Detailed error information with optional source evidence:

```java
public record ParseError(
    Range range,           // nullable when no source evidence is available
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
//   test}more
//       ^
```

An absent range is preserved: `new ParseError(null, "Invalid expression")`
formats as `error: Invalid expression`, even when source text is supplied. No
position or excerpt is invented. LSP diagnostics need a range; a tool must expose
an unlocated diagnostic through an appropriate separate channel.

`ParseError.toString()` and `formatWithSource()` display one-based line and
column numbers for people. This conversion is confined to error rendering;
stored coordinates and `Position`/`Range` string representations stay zero-based.

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
- Uses `SourceText` for LF, CRLF and CR with zero-based UTF-16 coordinates
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
    if (error.range() == null) {
        throw new IllegalArgumentException("An unlocated error has no LSP range");
    }
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
            range.start().line(),    // Already zero-based
            range.start().column()
        ),
        new org.eclipse.lsp4j.Position(
            range.end().line(),
            range.end().column()
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
- **Source indexing**: O(n) setup, O(log lines) position lookup, O(1) code-point conversion
- **Error collection**: Amortized O(1) append with ArrayList
- **Immutable collections**: Defensive copies prevent mutations
- **Records**: Efficient memory layout and fast equality checks

## Requirements

- Java 23 or higher (for sealed interfaces and records)
- No external dependencies

## Test Coverage

Tests cover:
- Position validation and comparison
- Range creation and validation
- ParseError formatting and severity
- ParseResult pattern matching and composition
- ParseException formatting
- Peekable position tracking and newline handling
- Shared coordinates across LF/CRLF/CR, UTF-16 supplementary characters, and EOF
- Missing source evidence and bounded multiline diagnostic excerpts

## Migrating from 1.x to 2.0.0

This is a breaking coordinate correction. Version 1.x used one-based lines and
columns despite describing them as LSP-compatible. Version 2.0.0 uses zero-based
lines and UTF-16 columns throughout; offsets remain zero-based UTF-16 and ranges
remain half-open. Remove `- 1` conversions in LSP adapters and update literal
`Position` construction and expected structured diagnostic coordinates; human
error rendering remains one-based. Prefer `SourceText`
when deriving coordinates from text. CRLF is one line break and bare CR is now
recognized as a line break. `ParseError.range()` may be null when source evidence
is unavailable; render the message without inventing a location.

The repository shares one base version across modules, so the coordinated base
version is 2.0.0. The existing build adds branch/local suffixes except for tagged
release builds; see [versioning](../docs/VERSIONING.md).

## License

Part of the rege-language project.

## See Also

- [rege-core](../rege-core/README.md) - Regular expression parser using this infrastructure
- [package-info.java](src/main/java/rege/reader/infra/package-info.java) - Detailed API documentation
