package io.viglante.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InputStateEvaluator {
    @JsonProperty(defaultValue = "warp")
    private final String company;

    @JsonProperty(value="service", required = true)
    private final String serviceKey;

    @JsonProperty(value = "service-id", required = true)
    private final long serviceId;

    @JsonProperty(value="team-id", required = true)
    private final long teamId;

    @JsonProperty(value="incident", required = true)
    private final String incidentKey;

    @JsonProperty(required = true)
    private final IncidentState state;

    @JsonProperty(required = true)
    private final String description;

    @JsonProperty(required = true)
    private final Long timestamp;
}
