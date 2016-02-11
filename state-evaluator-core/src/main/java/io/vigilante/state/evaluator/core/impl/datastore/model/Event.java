package io.vigilante.state.evaluator.core.impl.datastore.model;

import io.viglante.common.model.IncidentState;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Event {
    private final long timestamp;
    private final IncidentState state;
}
