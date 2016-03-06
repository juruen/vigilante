package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class Team {
    private final Optional<String> id;
    private final String name;
    private final List<String> users;

    @JsonCreator
    public Team(@JsonProperty("id") Optional<String> id, @JsonProperty("name") String name,
                @JsonProperty("users") List<String> users) {
        this.id = id;
        this.name = name;
        this.users = Optional.ofNullable(users).orElse(ImmutableList.<String>of());
    }
}
