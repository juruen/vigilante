package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class Incidents {
    @JsonProperty
    private List<Incident> incident;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Incident {
        @JsonProperty
        private String id;

        @JsonProperty
        private String description;

        @JsonProperty
        private Teams.Team team;

        @JsonProperty
        private Services.Service service;

        @JsonProperty
        private String key;

        @JsonProperty
        private Long start;

        @JsonProperty
        private State state;

        public  enum State {
            RESOLVED,
            TRIGGERED,
            ACKNOWLEDGED
        }

    }
}
