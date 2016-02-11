package io.vigilante.site.api.impl.datastore.basic.schedule;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedule.ScheduleLevel;
import io.vigilante.ScheduleProtos.Schedule.TimeRange;
import io.vigilante.ScheduleProtos.Schedules;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryResult;

public class ScheduleOperations {

	private Datastore datastore;

	public ScheduleOperations(final Datastore datastore) {
		this.datastore = datastore;
	}

	public ListenableFuture<Schedules> getSchedules(final @NonNull String namespace) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.getSchedules()), buildSchedules());
	}

	public ListenableFuture<Long> addSchedule(final @NonNull String namespace, final @NonNull Schedule schedule) {
		ListenableFuture<MutationResult> insert = Futures.transform(verifySchedule(namespace, schedule),
				mutateSchedule(ScheduleQueries.insertSchedule(schedule)));

		ListenableFuture<Key> fetchKey = Futures.transform(insert, AsyncUtil.fetchInsertKey());

		return Futures.transform(fetchKey, addScheduleLeveles(namespace, schedule));
	}

	public ListenableFuture<Void> modifySchedule(final @NonNull String namespace, final long id,
			final @NonNull Schedule Schedule) {
		ListenableFuture<Void> delete = Futures.transform(verifySchedule(namespace, Schedule),
				deleteScheduleLevels(namespace, id));

		return Futures.transform(delete, updateSchedule(namespace, id, Schedule));
	}

	public ListenableFuture<Void> deleteSchedule(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.deleteSchedule(id)), AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Schedule> getSchedule(final @NonNull String namespace, final long id) {
		ListenableFuture<Schedule> basicSchedule = Futures.transform(
				datastore.executeAsync(ScheduleQueries.getSchedule(id)), buildSchedule());
		ListenableFuture<List<ScheduleLevel>> leveles = getScheduleLevels(namespace, id);

		return Futures.transform(Futures.successfulAsList(Arrays.asList(basicSchedule, leveles)),
				buildCompleteSchedule());
	}

	public ListenableFuture<Schedules> getSchedulesForUser(final @NonNull String namespace, final long id) {
		ListenableFuture<Schedules> schedules = Futures.transform(
				datastore.executeAsync(ScheduleQueries.getScheduleLevelsForUser(id)),
				fetchSchedulesFromLevels(namespace));

		return Futures.transform(schedules, removeSecondLevel());
	}

	public ListenableFuture<Schedules> getSchedulesForTeam(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.getSchedulesForTeam(id)), buildSchedules());
	}

	public ListenableFuture<Void> scheduleExists(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.getTeamKey(id)),
				AsyncUtil.keyExists("Schedule"));
	}

	private AsyncFunction<QueryResult, Schedules> fetchSchedulesFromLevels(final String namespace) {
		return new AsyncFunction<QueryResult, Schedules>() {

			@Override
			public ListenableFuture<Schedules> apply(QueryResult result) throws Exception {
				Set<Long> scheduleIds = new HashSet<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						scheduleIds.add(e.getKey().getPath().get(0).getId());
					}
				}

				List<ListenableFuture<Schedule>> schedules = new ArrayList<>();

				for (Long id : scheduleIds) {
					schedules.add(getSchedule(namespace, id));
				}

				return Futures.transform(Futures.allAsList(schedules), listToSchedules());
			}
		};
	}

	private AsyncFunction<Void, Void> updateSchedule(final String namespace, final long id, final Schedule schedule) {
		return new AsyncFunction<Void, Void>() {

			@Override
			public ListenableFuture<Void> apply(Void ignored) throws Exception {
				ListenableFuture<MutationResult> update = datastore.executeAsync(ScheduleQueries.updateSchedule(id,
						schedule));
				return Futures.transform(update, updateScheduleLevels(namespace, id, schedule));
			}
		};
	}

	private AsyncFunction<MutationResult, Void> updateScheduleLevels(final String namespace, final long id,
			final Schedule schedule) {
		return new AsyncFunction<MutationResult, Void>() {

			@Override
			public ListenableFuture<Void> apply(MutationResult result) throws Exception {
				Batch batch = ScheduleQueries.insertScheduleLevels(ScheduleQueries.getScheduleKey(id), schedule);
				return Futures.transform(datastore.executeAsync(batch), AsyncUtil.emptyResponse());
			}
		};
	}

	private AsyncFunction<List<Schedule>, Schedules> listToSchedules() {
		return new AsyncFunction<List<Schedule>, Schedules>() {

			@Override
			public ListenableFuture<Schedules> apply(List<Schedule> result) throws Exception {
				return Futures.immediateFuture(Schedules.newBuilder().addAllSchedules(result).build());
			}
		};
	}

	private ListenableFuture<List<ScheduleLevel>> getScheduleLevels(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.getScheduleLeveles(id)),
				buildScheduleLevels());
	}

	private ListenableFuture<List<Key>> getScheduleLevelKeys(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(ScheduleQueries.getScheduleLeveles(id)),
				buildScheduleLevelKeys());
	}

	private AsyncFunction<Schedule, Void> deleteScheduleLevels(final @NonNull String namespace, final long id) {
		return new AsyncFunction<Schedule, Void>() {

			@Override
			public ListenableFuture<Void> apply(Schedule input) throws Exception {
				return Futures.transform(getScheduleLevelKeys(namespace, id), doDeleteScheduleLevels());
			}
		};
	}

	private AsyncFunction<List<Key>, Void> doDeleteScheduleLevels() {
		return new AsyncFunction<List<Key>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<Key> keys) throws Exception {
				if (keys.isEmpty()) {
					return Futures.immediateFuture(null);
				}

				Batch batch = new Batch();

				for (Key key : keys) {
					batch.add(ScheduleQueries.deleteScheduleLevel(key));
				}

				return Futures.transform(datastore.executeAsync(batch), AsyncUtil.emptyResponse());
			}
		};
	}

	private ListenableFuture<Schedule> verifySchedule(String namespace, Schedule schedule) {
		if (schedule.getScheduleLevelsCount() == 0) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule levels are empty"));
		}

		if (schedule.getScheduleLevels(0).getTimeRangesCount() == 0) {
			return Futures.immediateFailedFuture(new SiteExternalException("Time ranges are empty"));
		}

		if (!schedule.hasLength()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule length is missing"));
		}

		if (!schedule.hasStart()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule start is missing"));
		}

		if (schedule.getLength() < 0) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule length must be positive"));
		}

		if (schedule.getStart() < 0) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule start must be positive"));
		}

		if (!schedule.hasTeam()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Team is missing"));

		}

		if (!schedule.getTeam().hasId()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Team id is missing"));
		}

		return Futures.immediateFuture(schedule);
	}

	private AsyncFunction<QueryResult, Schedules> buildSchedules() {
		return new AsyncFunction<QueryResult, Schedules>() {

			@Override
			public ListenableFuture<Schedules> apply(QueryResult result) throws Exception {
				List<Schedule> schedules = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						schedules.add(Schedule.newBuilder()
								.setId(e.getKey().getId())
								.setName(e.getString(Constants.NAME))
								.setStart(e.getInteger(Constants.START))
								.setLength(e.getInteger(Constants.LENGTH))
								.setTeam(Team.newBuilder().setId(e.getInteger(Constants.TEAM)).build())
								.build());
					}
				}

				return Futures.immediateFuture(Schedules.newBuilder().addAllSchedules(schedules).build());
			}
		};
	}

	private AsyncFunction<QueryResult, List<ScheduleLevel>> buildScheduleLevels() {
		return new AsyncFunction<QueryResult, List<ScheduleLevel>>() {

			@Override
			public ListenableFuture<List<ScheduleLevel>> apply(QueryResult result) throws Exception {
				List<TimeRangeLevel> ranges = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						TimeRangeLevel range = new TimeRangeLevel(TimeRange.newBuilder()
								.setStart(e.getInteger(Constants.START))
								.setLength(e.getInteger(Constants.LENGTH))
								.setUser(User.newBuilder().setId(e.getInteger(Constants.USER)))
								.build(),
								e.getInteger(Constants.LEVEL).intValue());

						ranges.add(range);
					}
				}

				List<ScheduleLevel> scheduleLevels = new ArrayList<>();

				sortRanges(ranges);

				if (!ranges.isEmpty()) {
					int level = ranges.get(0).getLevel();

					List<TimeRange> timeRanges = new ArrayList<>();

					for (TimeRangeLevel trLevel : ranges) {
						int curr = trLevel.getLevel();

						if (curr != level) {
							scheduleLevels.add(ScheduleLevel.newBuilder().setLevel(level).addAllTimeRanges(timeRanges)
									.build());

							timeRanges = new ArrayList<>();

							level = curr;
						}

						timeRanges.add(trLevel.getTimeRange());
					}

					if (!timeRanges.isEmpty()) {
						scheduleLevels.add(ScheduleLevel.newBuilder().setLevel(level).addAllTimeRanges(timeRanges)
								.build());
					}
				}

				return Futures.immediateFuture(scheduleLevels);
			}
		};
	}

	private AsyncFunction<QueryResult, List<Key>> buildScheduleLevelKeys() {
		return new AsyncFunction<QueryResult, List<Key>>() {

			@Override
			public ListenableFuture<List<Key>> apply(QueryResult result) throws Exception {
				List<Key> keys = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						keys.add(e.getKey());
					}
				}

				return Futures.immediateFuture(keys);
			}
		};
	}

	private AsyncFunction<Schedule, MutationResult> mutateSchedule(final MutationStatement mutation) {
		return new AsyncFunction<Schedule, MutationResult>() {

			@Override
			public ListenableFuture<MutationResult> apply(Schedule schedule) throws Exception {
				return datastore.executeAsync(mutation);
			}
		};
	}

	private AsyncFunction<QueryResult, Schedule> buildSchedule() {
		return new AsyncFunction<QueryResult, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(QueryResult result) throws Exception {
				Entity e = result.getEntity();

				if (e == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Schedule doesn't exist"));
				}

				return Futures.immediateFuture(ScheduleUtil.buildBasicSchedule(e));
			}
		};
	}

	private AsyncFunction<List<Object>, Schedule> buildCompleteSchedule() {
		return new AsyncFunction<List<Object>, Schedule>() {

			@Override
			public ListenableFuture<Schedule> apply(List<Object> results) throws Exception {
				if (results.get(0) == null) {
					return Futures.immediateFailedFuture(new SiteExternalException(
							"Schudele doesn't exist or couldn't be fetched"));
				}

				Schedule schedule = (Schedule) results.get(0);

				if (results.get(1) == null) {
					return Futures.immediateFuture(schedule);
				}

				@SuppressWarnings("unchecked")
				List<ScheduleLevel> levels = (List<ScheduleLevel>) results.get(1);

				return Futures.immediateFuture(schedule.toBuilder().addAllScheduleLevels(levels).build());
			}
		};
	}

	private AsyncFunction<Key, Long> addScheduleLeveles(final String namespace, final Schedule schedule) {
		return new AsyncFunction<Key, Long>() {

			@Override
			public ListenableFuture<Long> apply(final Key key) throws Exception {
				Batch batch = ScheduleQueries.insertScheduleLevels(key, schedule);
				return Futures.transform(datastore.executeAsync(batch), AsyncUtil.returnId(key.getId()));
			}
		};
	}

	private void sortRanges(List<TimeRangeLevel> ranges) {
		ranges.sort(new Comparator<TimeRangeLevel>() {

			@Override
			public int compare(TimeRangeLevel o1, TimeRangeLevel o2) {
				if (o1.getLevel() < o2.getLevel()) {
					return -1;
				} else if (o1.getLevel() > o2.getLevel()) {
					return 1;
				} else if (o1.getTimeRange().getStart() < o2.getTimeRange().getStart()) {
					return -1;
				} else if (o1.getTimeRange().getStart() > o2.getTimeRange().getStart()) {
					return 1;
				}
				return 0;
			}
		});
	}

	private AsyncFunction<Schedules, Schedules> removeSecondLevel() {
		return new AsyncFunction<Schedules, Schedules>() {

			@Override
			public ListenableFuture<Schedules> apply(Schedules schedules) throws Exception {
				List<Schedule> newSchedules = new ArrayList<>();

				for (Schedule schedule : schedules.getSchedulesList()) {
					newSchedules.add(schedule.toBuilder().clearScheduleLevels().clearScheduleLevels().build());
				}

				return Futures.immediateFuture(Schedules.newBuilder().addAllSchedules(newSchedules).build());
			}
		};
	}
}
