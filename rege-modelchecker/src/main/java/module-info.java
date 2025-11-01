/**
 * Provides model checking capabilities for verifying system properties using regular expressions.
 * 
 * <p>This module integrates the OBP3 model checking framework with regular expression-based
 * property specifications, enabling automated verification of temporal properties expressed
 * as regular expressions over atomic propositions using Brzozowski derivatives.
 * 
 * @see rege.modelchecker.StepModelChecker
 * @see rege.modelchecker.DependentSemantics
 */
module language.rege.modelchecker {
    requires transitive obp.sli.runtime;
    requires obp.algos;
    requires reader.infra;
    requires language.rege.core;
    exports rege.modelchecker;

}