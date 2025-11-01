package rege.modelchecker;

import obp3.runtime.sli.SemanticRelation;
import obp3.runtime.sli.Step;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NBitsSLIMock implements SemanticRelation<Integer, Integer> {
    final int max;
    Optional<Integer> maybeDeadlockID;
    public NBitsSLIMock(int max, Optional<Integer> maybeDeadlockID) {
        //ensure that max in [0..Integer.SIZE]
        this.max = Math.max(0, Math.min(Integer.SIZE, max));
        this.maybeDeadlockID = maybeDeadlockID;
    }
    public NBitsSLIMock(int max) {
        this(max, Optional.empty());
    }

    public NBitsSLIMock(int max, int deadlockId) {
        this(max, Optional.of(deadlockId));
    }

    @Override
    public List<Integer> initial() {
        return List.of(0);
    }

    @Override
    public List<Integer> actions(Integer configuration) {
        if (maybeDeadlockID.map(configuration::equals).orElse(false)) {return List.of();}
        //for each bit position generate an action.
        return IntStream.range(0, max).boxed().collect(Collectors.toList());
    }

    @Override
    public List<Integer> execute(Integer action, Integer configuration) {
        //toggles bit at action position
        return List.of(configuration ^ (1 << action));
    }


    /// N Bits diagnosis language
    /// given a step e=(23, 4, 7)
    /// a single number -->  matches against the source state
    ///  - eval("23", e) -> true
    ///  - eval("24", e) -> false
    ///
    /// 't' followed by a number --> matches against the target state
    /// - eval(t7, e) -> true
    /// - eval(t23, e) -> false
    ///
    /// 'adeadlock' --> matches against a step (x, Optional.Empty, x)'
    /// 'a4' --> matches against a step (x, Optional.of(4), x)'

    public static boolean stepAtomEvaluator(String atom, Step<Integer, Integer> step) {
        try {
            switch (atom.charAt(0)) {
                case 'a':
                    if (atom.substring(1).equals("deadlock")) {
                        return step.action().isEmpty();
                    }
                    return Integer.parseInt(atom.substring(1)) == step.action().get();
                case 't': return Integer.parseInt(atom.substring(1)) == step.end();
                default: return Integer.parseInt(atom) == step.start();
            }
        } catch (Exception _) {
            throw new RuntimeException("Atom '%s' evaluation failed on step %s.".formatted(atom, step));
        }
    }
}
