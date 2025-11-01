/**
 * Core regular expression language implementation with syntax and semantics.
 * 
 * <p>This module provides the fundamental components for working with regular expressions,
 * including abstract syntax tree representation, parsing, and Brzozowski derivative-based
 * semantics for regular expression evaluation.
 * 
 * @see rege.syntax.RegeReader
 * @see rege.syntax.model.Expression
 * @see rege.semantics.Brzozowski
 */
module language.rege.core {
    requires reader.infra;
    exports rege.syntax;
    exports rege.syntax.model;
    exports rege.semantics;
}