package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequest {
    @JsonProperty
    private final String token;
}
