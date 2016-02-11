package io.vigilante.site.http.model.adapters;


import io.vigilante.ScheduleProtos;
import io.vigilante.site.http.model.Schedules;
import io.vigilante.site.http.model.Schedules;

import java.util.stream.Collectors;

public class SchedulesAdapters {
    public static Schedules fromProto(ScheduleProtos.Schedules schedules) {
        return Schedules.builder()
            .schedules(schedules.getSchedulesList()
                .stream()
                .map(SchedulesAdapters::fromProto)
                .collect(Collectors.toList()))
            .build();
    }

    public static Schedules.Schedule fromProto(ScheduleProtos.Schedule schedule) {
       return Schedules.Schedule.builder()
           .id(schedule.getId())
           .team(TeamsAdapters.fromProto(schedule.getTeam()))
           .name(schedule.getName())
           .start(schedule.getStart())
           .length(schedule.getLength())
           .scheduleLevels(schedule.getScheduleLevelsList()
               .stream()
               .map(SchedulesAdapters::fromProto)
               .collect(Collectors.toList()))
           .build();
    }

    private static Schedules.Schedule.ScheduleLevel fromProto(ScheduleProtos.Schedule.ScheduleLevel s) {
        return Schedules.Schedule.ScheduleLevel.builder()
            .level(s.getLevel())
            .timeRanges(s.getTimeRangesList()
                .stream()
                .map(SchedulesAdapters::fromProto)
                .collect(Collectors.toList()))
            .build();
    }

    private static Schedules.Schedule.TimeRange fromProto(ScheduleProtos.Schedule.TimeRange r) {
        return Schedules.Schedule.TimeRange.builder()
            .start(r.getStart())
            .length(r.getLength())
            .user(UsersAdapters.fromProto(r.getUser()))
            .build();
    }

    public static ScheduleProtos.Schedules fromPojo(Schedules schedules) {
        return ScheduleProtos.Schedules.newBuilder()
            .addAllSchedules(schedules.getSchedules()
                .stream()
                .map(SchedulesAdapters::fromPojo)
                .collect(Collectors.toList()))
            .build();
    }

    public static ScheduleProtos.Schedule fromPojo(Schedules.Schedule schedule) {
        ScheduleProtos.Schedule.Builder scheduleBuilder =  ScheduleProtos.Schedule.newBuilder();

        if (schedule.getId() != null) {
            scheduleBuilder.setId(schedule.getId());
        }

        if (schedule.getTeam() != null) {
            scheduleBuilder.setTeam(TeamsAdapters.fromPojo(schedule.getTeam()));
        }

        if (schedule.getName() != null) {
            scheduleBuilder.setName(schedule.getName());
        }

        if (schedule.getStart() != null) {
            scheduleBuilder.setStart(schedule.getStart());
        }

        if (schedule.getLength() != null) {
            scheduleBuilder.setLength(schedule.getLength());
        }

        scheduleBuilder.addAllScheduleLevels(schedule.getScheduleLevels()
            .stream().map(SchedulesAdapters::fromPojo)
            .collect(Collectors.toList()));

        return scheduleBuilder.build();
    }

    private static ScheduleProtos.Schedule.ScheduleLevel fromPojo(Schedules.Schedule.ScheduleLevel s) {
        return ScheduleProtos.Schedule.ScheduleLevel.newBuilder()
            .setLevel(s.getLevel())
            .addAllTimeRanges(s.getTimeRanges().stream().map(SchedulesAdapters::fromPojo).collect(Collectors.toList()))
            .build();
    }

    private static ScheduleProtos.Schedule.TimeRange fromPojo(Schedules.Schedule.TimeRange r) {
        return ScheduleProtos.Schedule.TimeRange.newBuilder()
            .setStart(r.getStart())
            .setLength(r.getLength())
            .setUser(UsersAdapters.fromPojo(r.getUser()))
            .build();
    }
    
}
