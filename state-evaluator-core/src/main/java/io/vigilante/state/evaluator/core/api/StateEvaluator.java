package io.vigilante.state.evaluator.core.api;


import com.google.common.util.concurrent.ListenableFuture;
import io.viglante.common.model.InputStateEvaluator;
import io.viglante.common.model.OutputStateEvaluator;

public interface StateEvaluator {
    ListenableFuture<OutputStateEvaluator> evaluate(InputStateEvaluator input);
}
