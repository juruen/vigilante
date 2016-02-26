package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class Schedule {
    private final Optional<Long> id;
    private final String name;
    private final long start;
    private final long length;
    private final List<TimeRange> timeRanges;

    @JsonCreator
    public Schedule(@JsonProperty("id") Optional<Long> id,
                    @JsonProperty("name") String name,
                    @JsonProperty("start") long start,
                    @JsonProperty("length") long length,
                    @JsonProperty("timeRanges") List<TimeRange> timeRanges) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.length = length;
        this.timeRanges = timeRanges;
    }

    public static class TimeRange {
        private final long user;
        private final long start;
        private final long length;

        @JsonCreator
        public TimeRange(@JsonProperty("user") long user,
                         @JsonProperty("start") long start,
                         @JsonProperty("length") long length) {
            this.user = user;
            this.start = start;
            this.length = length;
        }
    }
}
