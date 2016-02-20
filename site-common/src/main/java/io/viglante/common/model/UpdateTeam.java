package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateTeam {
    private final String name;
    private final List<Long> users;

    @JsonCreator
    public UpdateTeam(@JsonProperty("name") String name, @JsonProperty("users") List<Long> users) {
        this.name = name;
        this.users = users;
    }
}
