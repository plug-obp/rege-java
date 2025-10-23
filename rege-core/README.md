# Regular Expression Syntax Package

A comprehensive Java 23 implementation of regular expression syntax with parsers, algebraic simplification, and advanced pattern matching capabilities.

## 📦 Package Structure

```
rege.syntax/
├── model/              # Core syntax model (sealed types, records)
│   ├── Expression      # Base interface with smart constructors
│   ├── Terminal        # Atomic expressions (Token, Empty, Epsilon)
│   ├── Composite       # Compound expressions (Union, Concatenation, KleeneStar)
│   ├── Visitor         # Generic visitor pattern interface
│   └── Simplifier      # Algebraic simplification visitor
├── RegeReader          # Right-associative parser
└── RegeReaderLeft      # Left-associative parser
```

---

## 🏗️ Syntax Model (`model/`)

### Type Hierarchy

The syntax model uses **Java 23 sealed types** to create an exhaustive, type-safe expression tree:

```
Expression (sealed interface)
├── Terminal (sealed interface)
│   ├── Token(String value)              # record
│   ├── Empty                            # singleton
│   └── Epsilon                          # singleton
└── Composite (sealed interface)
    ├── Union(Expression lhs, rhs)       # record
    ├── Concatenation(Expression lhs, rhs) # record
    └── KleeneStar(Expression expr)      # record
```

### Expression Types

#### **Expression** (Base Interface)
- **Purpose**: Root of the sealed type hierarchy
- **Key Features**:
  - Smart constructors: `union()`, `concat()`, `star()`
  - Factory methods with built-in simplification rules
  - Visitor pattern support: `accept(Visitor<T,R> visitor, T input)`
  - Constants: `Expression.EMPTY`, `Expression.EPSILON`

#### **Terminal** (Atomic Expressions)

| Type | Notation | Description | Implementation |
|------|----------|-------------|----------------|
| **Token** | `τ[value]` | Matches a specific string | `record Token(String value)` |
| **Empty** | `∅` | Empty language (matches nothing) | Singleton with identity-based `equals()` |
| **Epsilon** | `ε` | Empty string (matches "") | Singleton with identity-based `equals()` |

**Singleton Design**: `Empty` and `Epsilon` use the singleton pattern with serialization protection via `readResolve()`.

#### **Composite** (Compound Expressions)

| Type | Notation | Description | Implementation | Equality |
|------|----------|-------------|----------------|----------|
| **Union** | `A \| B` | Choice between A or B | `record Union(Expression lhs, rhs)` | Commutative* |
| **Concatenation** | `A⋅B` | A followed by B | `record Concatenation(Expression lhs, rhs)` | Standard |
| **KleeneStar** | `A*` | Zero or more repetitions of A | `record KleeneStar(Expression expr)` | Standard |

**\*Commutative Equality**: `Union` implements custom `equals()` and `hashCode()` to ensure `A|B` equals `B|A`.

### Smart Constructors

The `Expression` interface provides factory methods that apply **algebraic simplification laws**:

```java
// Union laws
∅ | A = A              // Identity
A | A = A              // Idempotent  
A | (A | B) = A | B    // Absorption (1-level)

// Concatenation laws
∅⋅A = ∅                // Annihilator
ε⋅A = A                // Identity
A⋅ε = A                // Identity

// Kleene star laws
∅* = ε                 // Empty star
ε* = ε                 // Epsilon star
(A*)* = A*             // Idempotent
```

**Usage**:
```java
Expression.EMPTY.union(new Token("a"))     // → Token("a")
Expression.EPSILON.concat(new Token("b"))  // → Token("b")
new Token("x").star().star()               // → KleeneStar(Token("x"))
```

---

## 🔍 Simplifier (`model/Simplifier`)

A **visitor-based algebraic simplifier** that recursively simplifies regular expressions using mathematical laws.

### Features

- **Bottom-up simplification**: Recursively simplifies sub-expressions first
- **Deep absorption**: Handles arbitrarily nested unions (e.g., `A | (B | (A | C))` → `B | A | C`)
- **All algebraic laws**: Applies union, concatenation, and Kleene star laws
- **Immutable**: Returns new simplified expressions without modifying originals

### Usage

```java
Expression complex = new Union(
    new Token("a"),
    new Union(new Token("a"), new Token("b"))  // a | (a | b)
);

Simplifier simplifier = new Simplifier();
Expression simplified = complex.accept(simplifier, null);
// Result: Union(Token("a"), Token("b"))  // a | b
```

### Simplification Rules

| Input | Output | Law |
|-------|--------|-----|
| `∅ \| A` | `A` | Union identity |
| `A \| A` | `A` | Union idempotent |
| `A \| (A \| B)` | `A \| B` | Union absorption |
| `∅⋅A` | `∅` | Concatenation annihilator |
| `ε⋅A` | `A` | Concatenation identity |
| `∅*` | `ε` | Kleene star of empty |
| `ε*` | `ε` | Kleene star of epsilon |
| `(A*)*` | `A*` | Kleene star idempotent |

**Test Coverage**: 41 tests (SimplifierTest: 25, SimplifierAbsorptionTest: 16)

---

## 📖 Parsers

### RegeReader (Right-Associative)

**Default parser** that produces right-associative parse trees using recursive descent.

#### Grammar

```
E  -> ∅ E'              # empty set
    | ϵ E'              # epsilon
    | τ[string] E'      # token
    | t[string] E'      # token (alternative)
    | (E) E'            # parentheses

E' -> | E E'            # union
    | ∪ E E'            # union (alternative)
    | . E E'            # concatenation
    | ⋅ E E'            # concatenation (alternative)
    | E E'              # implicit concatenation
    | * E'              # Kleene star
    | ε                 # end of input
```

#### Associativity

```java
RegeReader.readExpression("a.b.c", false)
// → Concat(Token("a"), Concat(Token("b"), Token("c")))
// → a⋅(b⋅c)  [RIGHT-associative]

RegeReader.readExpression("a|b|c", false)  
// → Union(Token("a"), Union(Token("b"), Token("c")))
// → a|(b|c)  [RIGHT-associative]
```

#### Operator Precedence (Highest to Lowest)

1. **Kleene Star** (`*`) - highest precedence
2. **Concatenation** (`.`, `⋅`, or implicit)
3. **Union** (`|`, `∪`) - lowest precedence
4. **Parentheses** `()` - override precedence

**Example**: `a|b.c*` parses as `a|(b⋅(c*))`, not `((a|b)⋅c)*`

#### Usage

```java
// With smart constructors (default)
Expression expr = RegeReader.readExpression("τ[hello]|ϵ");

// Without smart constructors
Expression raw = RegeReader.readExpression("∅*", false);
// raw = KleeneStar(Empty)

Expression smart = RegeReader.readExpression("∅*", true);
// smart = Epsilon (simplified)
```

**Test Coverage**: 57 tests

---

### RegeReaderLeft (Left-Associative)

**Alternative parser** that produces left-associative parse trees using iterative loops.

#### Why Left-Associative?

Left-associative trees are more natural for **left-to-right algorithms**:
- **Brzozowski derivatives**: "Peel off" symbols from the left
- **Streaming/sequential processing**: Process prefix first
- **Natural traversal**: Matches reading order

#### Associativity Comparison

```java
// Same input, different structures
String input = "a.b.c";

RegeReader.readExpression(input, false)
// → a⋅(b⋅c)  [right-heavy tree]

RegeReaderLeft.readExpression(input, false)  
// → (a⋅b)⋅c  [left-heavy tree]
```

| Aspect | RegeReader | RegeReaderLeft |
|--------|------------|----------------|
| Parse style | Recursive descent | Iterative loops |
| Concatenation | `a⋅(b⋅c)` | `(a⋅b)⋅c` |
| Union | `a\|(b\|c)` | `(a\|b)\|c` |
| Best for | Composition, natural parsing | Derivatives, left-to-right |
| Tree shape | Right-heavy | Left-heavy |

#### Semantic Equivalence

**Both parsers are semantically equivalent** (accept the same languages):

```java
// Different structures
RegeReader.readExpression("a.b.c", false)      // a⋅(b⋅c)
RegeReaderLeft.readExpression("a.b.c", false)  // (a⋅b)⋅c
// Both match the same strings: ["abc"]

// Smart constructors produce IDENTICAL results
RegeReader.readExpression("∅*", true)      // ε
RegeReaderLeft.readExpression("∅*", true)  // ε
```

#### Usage

```java
// Left-associative parsing
Expression left = RegeReaderLeft.readExpression("a.b.c.d", false);
// → (((a⋅b)⋅c)⋅d)  [deep left spine]

// Smart constructors work identically
Expression simplified = RegeReaderLeft.readExpression("ϵ|∅");
// → Epsilon
```

**Test Coverage**: 31 tests + 5 comparison tests

---

## 🎯 Key Design Patterns

### 1. **Sealed Types** (Exhaustive Pattern Matching)
```java
public sealed interface Expression permits Terminal, Composite {
    // Compiler ensures all cases are handled
}
```

### 2. **Visitor Pattern** (Extensible Traversal)
```java
public interface Visitor<T, R> {
    R visitToken(Token token, T input);
    R visitEmpty(Empty empty, T input);
    // ... all expression types
}
```

### 3. **Singleton Pattern** (Identity-Based Equality)
```java
public final class Empty implements Terminal {
    private static final Empty INSTANCE = new Empty();
    public static Empty instance() { return INSTANCE; }
    private Object readResolve() { return INSTANCE; }  // Serialization safety
}
```

### 4. **Smart Constructors** (Algebraic Simplification)
```java
default Expression concat(Expression other) {
    if (this == EMPTY || other == EMPTY) return EMPTY;  // Annihilator
    if (this == EPSILON) return other;                  // Identity
    return new Concatenation(this, other);
}
```

---

## 📊 Test Coverage

### Model Tests (64 tests)
- **EqualsHashCodeTest** (13 tests): Verifies correct `equals()` and `hashCode()` implementations
- **SimplifierTest** (25 tests): Tests algebraic simplification rules
- **SimplifierAbsorptionTest** (16 tests): Tests deep absorption (arbitrary nesting)
- **UnionAbsorptionTest** (10 tests): Tests 1-level absorption in factory methods

### Parser Tests (93 tests)
- **RegeReaderTest** (57 tests): Right-associative parser
- **RegeReaderLeftTest** (31 tests): Left-associative parser
- **RegeReaderComparisonTest** (5 tests): Structural differences and semantic equivalence

**Total: 157 tests** ✅

---

## 🚀 Quick Start

### Parse and Simplify

```java
// Parse with automatic simplification
Expression expr = RegeReader.readExpression("(a|b)*⋅c");

// Manual simplification
Expression complex = new Union(
    Expression.EMPTY,
    new Token("x")
);
Expression simple = complex.accept(new Simplifier(), null);
// Result: Token("x")
```

### Compare Associativity

```java
String input = "a.b.c.d";

Expression right = RegeReader.readExpression(input, false);
// → a⋅(b⋅(c⋅d))  [shallow left spine]

Expression left = RegeReaderLeft.readExpression(input, false);
// → (((a⋅b)⋅c)⋅d)  [deep left spine]

// Both are semantically equivalent but structurally different
```

### Use Smart Constructors

```java
// Direct construction with simplification
Expression simplified = 
    Expression.EPSILON
        .concat(new Token("hello"))
        .union(Expression.EMPTY);
// Result: Token("hello")

// vs. raw construction (no simplification)
Expression raw = new Union(
    new Concatenation(Expression.EPSILON, new Token("hello")),
    Expression.EMPTY
);
// Result: Union(Concat(Epsilon, Token("hello")), Empty)
```

---

## 🔬 Advanced Features

### Commutative Union Equality

```java
Union u1 = new Union(new Token("a"), new Token("b"));
Union u2 = new Union(new Token("b"), new Token("a"));

u1.equals(u2);  // → true (commutative)
u1.hashCode() == u2.hashCode();  // → true (consistent)
```

**Implementation**: Custom `hashCode()` using symmetric formula:
```java
31 * (h1 + h2) + (h1 * h2)
```

### Deep Absorption

The `Simplifier` handles arbitrarily nested unions:

```java
Expression nested = new Union(
    new Token("a"),
    new Union(
        new Token("b"),
        new Union(new Token("a"), new Token("c"))  // "a" appears deep inside
    )
);

Expression simplified = nested.accept(new Simplifier(), null);
// Result: Union(Token("b"), Union(Token("a"), Token("c")))
// "a" absorbed from nested position
```

### Serialization Safety

Singletons remain singletons after serialization:

```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ObjectOutputStream oos = new ObjectOutputStream(baos);
oos.writeObject(Expression.EMPTY);

ObjectInputStream ois = new ObjectInputStream(
    new ByteArrayInputStream(baos.toByteArray())
);
Object deserialized = ois.readObject();

deserialized == Expression.EMPTY;  // → true (same instance)
```

---

## 🎓 Mathematical Foundations

### Regular Expression Algebra

The implementation follows standard regular expression algebra:

**Associativity**:
- `(A|B)|C = A|(B|C)` (Union is associative)
- `(A⋅B)⋅C = A⋅(B⋅C)` (Concatenation is associative)

**Commutativity**:
- `A|B = B|A` (Union is commutative) ✓ Implemented
- `A⋅B ≠ B⋅A` (Concatenation is NOT commutative)

**Identity Elements**:
- `∅|A = A` (∅ is identity for union)
- `ε⋅A = A` (ε is identity for concatenation)

**Annihilators**:
- `∅⋅A = ∅` (∅ annihilates concatenation)

### Language Semantics

| Expression | Language L(E) | Example |
|------------|---------------|---------|
| `∅` | `{}` (empty set) | Matches nothing |
| `ε` | `{""}` (set containing empty string) | Matches "" |
| `τ[abc]` | `{"abc"}` | Matches "abc" |
| `A\|B` | `L(A) ∪ L(B)` | Union of languages |
| `A⋅B` | `L(A) · L(B)` | Concatenation of languages |
| `A*` | `(L(A))*` | Kleene closure |

---

## 🛠️ Implementation Notes

### Why Java 23?

- **Sealed interfaces**: Exhaustive pattern matching (compiler-verified)
- **Records**: Immutable data with automatic `equals()`, `hashCode()`, `toString()`
- **Pattern matching**: Readiness for future switch expressions on sealed types

### Performance Considerations

- **Immutable structures**: All expressions are immutable (thread-safe)
- **Singleton optimization**: Empty and Epsilon use single instances
- **Lazy simplification**: Smart constructors apply rules eagerly, but Simplifier is on-demand
- **Canonicalization**: Union orders operands by hash code for consistent structure

### Extensibility

Add new visitors without modifying existing code:

```java
public class MyCustomVisitor implements Visitor<Void, String> {
    @Override
    public String visitToken(Token token, Void input) {
        return "Token: " + token.value();
    }
    // ... implement other visit methods
}

Expression expr = RegeReader.readExpression("τ[hello]");
String result = expr.accept(new MyCustomVisitor(), null);
```

---

## 📚 API Summary

### Core Types
- `Expression` - Base interface with smart constructors
- `Terminal` - Atomic expressions (Token, Empty, Epsilon)
- `Composite` - Compound expressions (Union, Concatenation, KleeneStar)
- `Visitor<T,R>` - Generic visitor interface

### Parsers
- `RegeReader.readExpression(String)` - Right-associative parser
- `RegeReaderLeft.readExpression(String)` - Left-associative parser

### Utilities
- `Simplifier` - Algebraic simplification visitor
- Smart constructors: `expr.union(other)`, `expr.concat(other)`, `expr.star()`

---

## 🎯 Use Cases

1. **Regular Expression Analysis**: Parse and simplify complex patterns
2. **Brzozowski Derivatives**: Use left-associative parser for derivative computation
3. **Pattern Matching**: Leverage sealed types for exhaustive case analysis
4. **Formal Verification**: Algebraic simplification for equivalence checking
5. **Educational**: Demonstrate regular expression theory with clean implementation

---

## 📖 References

- **Sealed Types**: [JEP 409](https://openjdk.org/jeps/409)
- **Records**: [JEP 395](https://openjdk.org/jeps/395)
- **Regular Expression Algebra**: Brzozowski, "Derivatives of Regular Expressions" (1964)
- **Visitor Pattern**: Gang of Four Design Patterns

---

**License**: Part of the rege-java project  
**Version**: Java 23  
**Test Coverage**: 157 tests, all passing ✅
