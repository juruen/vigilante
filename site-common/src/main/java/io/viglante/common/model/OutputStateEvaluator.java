package io.viglante.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class OutputStateEvaluator {

    @JsonProperty(required = true)
    private final String company;

    @JsonProperty(required = true)
    private final String service;

    @JsonProperty(value = "service-id", required = true)
    private final long serviceId;

    @JsonProperty(value="team-id", required = true)
    private final long teamId;

    @JsonProperty(required = true)
    private final String incident;

    @JsonProperty(required = true)
    private final IncidentState state;

    @JsonProperty(required = true)
    private final String description;

    @JsonProperty(required = true, value="sequence-number")
    private final Long sequenceNumber;

    @JsonProperty(required = true, value="creation-token")
    private final Long creationToken;

    private final Boolean notify;

    private final String incidentId;
}
