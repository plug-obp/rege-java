package rege.modelchecker;

import obp3.runtime.sli.DependentSemanticRelation;
import rege.semantics.Brzozowski;
import rege.semantics.RegeDependentSemantics;
import rege.syntax.model.Expression;

import java.util.function.BiPredicate;

public class DependentSemantics<T>
        extends RegeDependentSemantics<T>
        implements DependentSemanticRelation<T, Brzozowski<T>, Expression> {

    public DependentSemantics(Expression expression, BiPredicate<String, T> evaluator) {
        super(expression, evaluator);
    }
}
