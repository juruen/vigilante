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
    private final Optional<Long> id;
    private final String name;
    private final List<Long> users;

    @JsonCreator
    public Team(@JsonProperty("id") Optional<Long> id, @JsonProperty("name") String name,
                @JsonProperty("users") List<Long> users) {
        this.id = id;
        this.name = name;
        this.users = Optional.ofNullable(users).orElse(ImmutableList.<Long>of());
    }
}
