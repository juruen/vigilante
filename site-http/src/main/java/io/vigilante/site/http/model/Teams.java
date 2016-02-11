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
public class Teams {
    @JsonProperty
    private List<Team> teams;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Team {
        @JsonProperty
        private Long id;

        @JsonProperty
        private String name;

        @JsonProperty
        private List<Users.User> users;
    }
}
