package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class MutateTeam {
    private final Optional<Long> id;
    private final String name;
    private final List<Long> users;

    @JsonCreator
    public MutateTeam(@JsonProperty("id") Optional<Long> id, @JsonProperty("name") String name,
                      @JsonProperty("users") List<Long> users) {
        this.id = id;
        this.name = name;
        this.users = users;
    }
}
