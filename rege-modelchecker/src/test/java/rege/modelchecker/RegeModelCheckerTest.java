package rege.modelchecker;

import obp3.modelchecking.EmptinessCheckerAnswer;
import obp3.modelchecking.EmptinessCheckerStatus;
import obp3.runtime.IExecutable;
import obp3.runtime.sli.Step;
import obp3.sli.core.operators.product.Product;
import obp3.utils.Either;
import org.junit.jupiter.api.Test;
import rege.syntax.model.Expression;

import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

public class RegeModelCheckerTest {

    BiPredicate<String, Step<Integer, Integer>> atomEvaluatorAdapter =  (atom, step) -> switch (atom) {
        case "true"  -> true;
        case "false" -> false;
        default      -> NBitsSLIMock.stepAtomEvaluator(atom, step);
    };

    IExecutable<EmptinessCheckerStatus, EmptinessCheckerAnswer<Product<Integer, Expression>>> mc(int max, int deadlock, String property) {
        var checker = new StepModelChecker<>(
                new NBitsSLIMock(max, deadlock),
                atomEvaluatorAdapter,
                property);
        return checker.modelChecker();
    }

    IExecutable<EmptinessCheckerStatus, EmptinessCheckerAnswer<Product<Integer, Expression>>> mc(int max, String property) {
        var checker = new StepModelChecker<>(
                new NBitsSLIMock(max),
                atomEvaluatorAdapter,
                property);
        return checker.modelChecker();
    }

    final String findRege0 = "τ[true]* ⋅ τ[0]";
    final String findRege23 = "τ[true]* ⋅ τ[23]";
    final String findRege2373 = "τ[true]* ⋅ τ[23]τ[7]t[3]";
    final String findRege2363 = "τ[true]* ⋅ τ[23]τ[6]t[3]";
    final String noDeadlockRege = "τ [true]* ⋅ τ [adeadlock]";
    final String findRegeF = "τ[true]* ⋅ (τ[23] | t[5] τ[true]* t[10]) τ[true]* τ[3]";
    final String fullExploration = "τ [true]* ⋅ τ [false]";

    @Test
    void testFind0() {
        var result = mc(5, findRege0).runAlone();
        assertFalse(result.holds);
        assertEquals(2, result.trace.size());
    }

    @Test
    void testFind23() {
        var result = mc(5, findRege23).runAlone();
        assertFalse(result.holds);
        assertEquals(28, result.trace.size());
    }

    @Test
    void testFind2373() {
        var result = mc(5, findRege2373).runAlone();
        assertFalse(result.holds);
        assertEquals(30, result.trace.size());
    }
    @Test
    void testFind2363() {
        var result = mc(5, findRege2363).runAlone();
        assertTrue(result.holds);
    }

    @Test
    void testFindF() {
        var result = mc(3, findRegeF).runAlone();
        assertTrue(result.holds);
    }

    @Test
    void testFindF1() {
        var result = mc(5, findRegeF).runAlone();
        assertFalse(result.holds);
        assertEquals(38, result.trace.size());
    }

    @Test
    void testNoDeadlock() {
        var result = mc(5, noDeadlockRege).runAlone();
        assertTrue(result.holds);
    }

    @Test
    void testDeadlock() {
        var result = mc(5, 31, noDeadlockRege).runAlone();
        assertFalse(result.holds);
        assertEquals(23, result.trace.size());
    }
}
