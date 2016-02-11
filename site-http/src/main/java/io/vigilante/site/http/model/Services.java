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
public class Services {
    @JsonProperty
    private List<Service> services;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Service {
        @JsonProperty
        private Long id;

        @JsonProperty
        private String name;

        @JsonProperty
        private String key;

        @JsonProperty
        private Teams.Team team;

        @JsonProperty
        private Schedules.Schedule schedule;
    }
}
