package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddTeam {
    private final String name;

    @JsonCreator
    public AddTeam(@JsonProperty("name") String name) {
        this.name = name;
    }
}
