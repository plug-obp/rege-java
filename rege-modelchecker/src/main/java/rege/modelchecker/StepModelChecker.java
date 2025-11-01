package rege.modelchecker;

import obp3.modelchecking.EmptinessCheckerAnswer;
import obp3.modelchecking.tools.ModelCheckerBuilder;
import obp3.runtime.IExecutable;
import obp3.runtime.sli.SemanticRelation;
import obp3.runtime.sli.Step;
import obp3.traversal.dfs.DepthFirstTraversal;
import rege.reader.infra.ParseResult;
import rege.semantics.Brzozowski;
import rege.semantics.RegeDependentSemantics;
import rege.syntax.RegeReader;
import rege.syntax.model.Expression;

import java.util.function.BiPredicate;

public class StepModelChecker<MA, MC> {
    //model SLI
    SemanticRelation<MA, MC> modelSemantics;
    BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator;

    //property
    Expression propertyModel;

    //options
    DepthFirstTraversal.Algorithm traversalAlgorithm;
    int depthBound;

    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            String property) {
        var model = RegeReader.parse(property);
        switch (model) {
            case ParseResult.Success<Expression> success -> {
                this.modelSemantics = modelSemantics;
                this.atomicPropositionEvaluator = atomicPropositionEvaluator;
                this.propertyModel = success.value();
                this.traversalAlgorithm = DepthFirstTraversal.Algorithm.WHILE;
                this.depthBound = -1;
            }
            case ParseResult.Failure<Expression> failure -> {
                throw new IllegalArgumentException("Failed to parse property: " + failure.formatErrors());
            }
        }
    }

    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            Expression propertyModel) {
        this(
                modelSemantics,
                atomicPropositionEvaluator,
                propertyModel,
                DepthFirstTraversal.Algorithm.WHILE,
                -1
        );
    }

    public StepModelChecker(
            SemanticRelation<MA, MC> modelSemantics,
            BiPredicate<String, Step<MA, MC>> atomicPropositionEvaluator,
            Expression propertyModel,
            DepthFirstTraversal.Algorithm traversal,
            int depthBound) {
        this.modelSemantics = modelSemantics;
        this.atomicPropositionEvaluator = atomicPropositionEvaluator;
        this.propertyModel = propertyModel;
        this.traversalAlgorithm = traversal;
        this.depthBound = depthBound;
    }

    public IExecutable<EmptinessCheckerAnswer<?>> modelChecker() {
        var propertySemantics = new DependentSemantics<>(propertyModel, atomicPropositionEvaluator);
        var builder =
                new ModelCheckerBuilder<MA, MC, Brzozowski<Step<MA, MC>>, Expression>()
                        .modelSemantics(modelSemantics)
                        .propertySemantics(propertySemantics)
                        .acceptingPredicateForProduct((c) -> RegeDependentSemantics.isAccepting(c.r()))
                        .buchi(false)
                        .traversalStrategy(traversalAlgorithm)
                        .depthBound(depthBound);
        return builder.modelChecker();
    }
}