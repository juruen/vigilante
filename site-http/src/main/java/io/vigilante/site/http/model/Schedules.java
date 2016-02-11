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
public class Schedules {
    @JsonProperty
    private List<Schedule> schedules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Schedule {
        @JsonProperty
        private Long id;

        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Teams.Team team;

        @JsonProperty
        private String name;

        @JsonProperty
        private Long start;

        @JsonProperty
        private Long length;

        @JsonProperty
        private List<ScheduleLevel> scheduleLevels;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TimeRange {
            @JsonProperty
            private Long start;

            @JsonProperty
            private Long length;

            @JsonProperty
            private Users.User user;

        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScheduleLevel {
            @JsonProperty
            private Integer level;

            @JsonProperty
            private List<TimeRange> timeRanges;

        }

    }
}
