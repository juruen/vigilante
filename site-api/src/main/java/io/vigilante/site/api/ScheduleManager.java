package io.vigilante.site.api;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedules;

import com.google.common.util.concurrent.ListenableFuture;

public interface ScheduleManager {

	ListenableFuture<Schedule> getSchedule(final String namespace, final long id);

	ListenableFuture<Schedules> getSchedules(final String namespace);

	ListenableFuture<Long> addSchedule(final String namespace, final Schedule schedule);

	ListenableFuture<Void> updateSchedule(final String namespace, final long id, final Schedule schedule);

	ListenableFuture<Void> deleteSchedule(final String namespace, final long id);

	ListenableFuture<Schedules> getSchedulesForUser(final String namespace, final long id);

	ListenableFuture<Schedules> getSchedulesForTeam(final String namespace, final long id);
}
