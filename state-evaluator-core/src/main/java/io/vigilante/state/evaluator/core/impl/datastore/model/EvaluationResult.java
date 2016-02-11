package io.vigilante.state.evaluator.core.impl.datastore.model;

import io.viglante.common.model.IncidentState;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EvaluationResult {
    private final IncidentState state;
    private final long timestamp;
    private final boolean notify;
    private final List<Long> timestamps;
    private final List<IncidentState> states;
}
