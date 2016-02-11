package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddResponse {
    @JsonProperty
    private final String id;

    @JsonProperty
    private final String message;
}
