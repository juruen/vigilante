package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModifyResponse {
    @JsonProperty
    final private String message;
}
