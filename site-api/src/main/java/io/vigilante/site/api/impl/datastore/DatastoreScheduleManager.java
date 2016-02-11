package io.vigilante.site.api.impl.datastore;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedule.ScheduleLevel;
import io.vigilante.ScheduleProtos.Schedule.ScheduleLevel.Builder;
import io.vigilante.ScheduleProtos.Schedule.TimeRange;
import io.vigilante.ScheduleProtos.Schedules;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.basic.schedule.ScheduleOperations;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;
import io.vigilante.site.impl.datastore.basic.user.UserOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;

@Slf4j
public class DatastoreScheduleManager implements ScheduleManager {

	private ScheduleOperations scheduleOperations;
	private UserOperations userOperations;
	private TeamOperations teamOperations;

	public DatastoreScheduleManager(Datastore datastore) {
		this.scheduleOperations = new ScheduleOperations(datastore);
		this.userOperations = new UserOperations(datastore);
		this.teamOperations = new TeamOperations(datastore);
	}

	@Override
	public ListenableFuture<Schedule> getSchedule(final String namespace, final long id) {
		ListenableFuture<Schedule> teamDecorated = Futures.transform(scheduleOperations.getSchedule(namespace, id),
				decorateTeam(namespace));
		return Futures.transform(teamDecorated, decorateUsers(namespace));
	}

	@Override
	public ListenableFuture<Schedules> getSchedules(String namespace) {
		return scheduleOperations.getSchedules(namespace);
	}

	@Override
	public ListenableFuture<Long> addSchedule(String namespace, Schedule schedule) {
		try {
			verifySchedule(schedule);
		} catch (Exception e) {
			return Futures.immediateFailedFuture(e);
		}

		ListenableFuture<Void> usersExist = userOperations.usersExist(namespace, getUsers(schedule));
		ListenableFuture<Void> teamExist = teamOperations.teamExists(namespace, schedule.getTeam().getId());
		ListenableFuture<Long> addSchedule = scheduleOperations.addSchedule(namespace, schedule);

		return AsyncUtil.conditionalAdd(addSchedule, teamExist, usersExist);
	}

	@Override
	public ListenableFuture<Void> updateSchedule(String namespace, long id, Schedule schedule) {
		try {
			verifySchedule(schedule);
		} catch (Exception e) {
			return Futures.immediateFailedFuture(e);
		}

		ListenableFuture<Void> usersExist = userOperations.usersExist(namespace, getUsers(schedule));
		ListenableFuture<Void> teamExist = teamOperations.teamExists(namespace, schedule.getTeam().getId());
		ListenableFuture<Void> updateSchedule = scheduleOperations.modifySchedule(namespace, id, schedule);

		return AsyncUtil.conditionalUpdate(updateSchedule, teamExist, usersExist);
	}

	private void verifySchedule(Schedule schedule) throws SiteExternalException {
		if (!schedule.hasTeam() || !schedule.getTeam().hasId()) {
			throw new SiteExternalException("Team is missing");
		}

		if (schedule.getScheduleLevelsCount() == 0) {
			throw new SiteExternalException("ScheduleLevel is missing");
		}

		for (ScheduleLevel level : schedule.getScheduleLevelsList()) {
			if (level.getTimeRangesCount() == 0) {
				throw new SiteExternalException("TimeRange is missing");
			}
			for (TimeRange range : level.getTimeRangesList()) {
				if (!range.hasUser() || !range.getUser().hasId()) {
					throw new SiteExternalException("User is missing");
				}
			}
		}
	}

	@Override
	public ListenableFuture<Void> deleteSchedule(String namespace, long id) {
		return scheduleOperations.deleteSchedule(namespace, id);
	}

	@Override
	public ListenableFuture<Schedules> getSchedulesForUser(String namespace, long id) {
		return scheduleOperations.getSchedulesForUser(namespace, id);
	}

	@Override
	public ListenableFuture<Schedules> getSchedulesForTeam(String namespace, long id) {
		return scheduleOperations.getSchedulesForTeam(namespace, id);
	}

	private List<User> getUsers(Schedule schedule) {
		List<User> users = new ArrayList<>();

		for (ScheduleLevel level : schedule.getScheduleLevelsList()) {
			for (TimeRange range : level.getTimeRangesList()) {
				users.add(User.newBuilder().setId(range.getUser().getId()).build());
			}
		}

		return users;
	}

	private AsyncFunction<Schedule, Schedule> decorateTeam(final String namespace) {
		return new AsyncFunction<Schedule, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(Schedule schedule) throws Exception {

				ListenableFuture<Team> teamWithFallback = Futures.withFallback(
						teamOperations.getTeam(namespace, schedule.getTeam().getId()), AsyncUtil.teamFallback());

				return Futures.transform(teamWithFallback, decorateTeam(schedule));
			}
		};
	}

	protected AsyncFunction<Team, Schedule> decorateTeam(final Schedule schedule) {
		return new AsyncFunction<Team, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(Team team) throws Exception {
				return Futures.immediateFuture(schedule.toBuilder().setTeam(team.toBuilder().clearUsers()).build());
			}
		};
	}

	private AsyncFunction<Schedule, Schedule> decorateUsers(final String namespace) {
		return new AsyncFunction<Schedule, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(final Schedule schedule) throws Exception {
				List<ListenableFuture<User>> users = new ArrayList<>();

				for (ScheduleLevel level : schedule.getScheduleLevelsList()) {
					for (TimeRange range : level.getTimeRangesList()) {
						users.add(userOperations.getUser(namespace, range.getUser().getId()));
					}
				}

				return Futures.transform(Futures.successfulAsList(users), decorateUsers(schedule));
			}
		};
	}

	protected AsyncFunction<List<User>, Schedule> decorateUsers(final Schedule schedule) {
		return new AsyncFunction<List<User>, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(List<User> users) throws Exception {
				Map<Long, User> uidToUser = usersToUidSet(users);

				List<ScheduleLevel> levels = new ArrayList<>();
				for (ScheduleLevel level : schedule.getScheduleLevelsList()) {
					Builder levelBuilder = level.toBuilder();

					for (int i = 0; i < level.getTimeRangesCount(); i++) {
						TimeRange range = level.getTimeRanges(i);

						User user = range.getUser();
						if (!uidToUser.containsKey(user.getId())) {
							log.warn("schedule {} contains user {} that doesn't exist", schedule.getId(), user.getId());
							levelBuilder.removeTimeRanges(i);
							continue;
						}

						levelBuilder.setTimeRanges(
								i,
								range.toBuilder().setUser(
										uidToUser.get(user.getId()).toBuilder().clearNotifications().build()));
					}

					levels.add(levelBuilder.build());
				}

				return Futures.immediateFuture(schedule.toBuilder().clearScheduleLevels().addAllScheduleLevels(levels)
						.build());
			}
		};
	}

	private Map<Long, User> usersToUidSet(List<User> users) {
		List<User> existingUsers = FluentIterable.from(users).filter(Predicates.notNull()).toList();

		Map<Long, User> uidToUser = new HashMap<>();

		for (User user : existingUsers) {
			uidToUser.put(user.getId(), user);
		}

		return uidToUser;
	}
}
