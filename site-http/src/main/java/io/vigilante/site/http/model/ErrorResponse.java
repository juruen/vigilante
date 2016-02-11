package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    @JsonProperty
    private final String message;
}
