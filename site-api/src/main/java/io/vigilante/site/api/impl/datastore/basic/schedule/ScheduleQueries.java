package io.vigilante.site.api.impl.datastore.basic.schedule;

import static com.spotify.asyncdatastoreclient.QueryBuilder.eq;
import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedule.ScheduleLevel;
import io.vigilante.ScheduleProtos.Schedule.TimeRange;
import io.vigilante.site.impl.datastore.basic.Constants;
import lombok.NonNull;

import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;

public class ScheduleQueries {

	public static Query getSchedules() {
		return QueryBuilder.query().kindOf(Constants.SCHEDULE_KIND);
	}

	public static Insert insertSchedule(Schedule schedule) {
		return QueryBuilder.insert(Constants.SCHEDULE_KIND)
				.value(Constants.NAME, schedule.getName())
				.value(Constants.START, schedule.getStart())
				.value(Constants.LENGTH, schedule.getLength())
				.value(Constants.TEAM, schedule.getTeam().getId());
	}

	public static MutationStatement updateSchedule(long id, Schedule schedule) {
		return QueryBuilder.update(Constants.SCHEDULE_KIND, id)
				.value(Constants.NAME, schedule.getName())
				.value(Constants.START, schedule.getStart())
				.value(Constants.LENGTH, schedule.getLength())
				.value(Constants.TEAM, schedule.getTeam().getId());
	}

	public static Delete deleteSchedule(final long id) {
		return QueryBuilder.delete(Constants.SCHEDULE_KIND, id);
	}

	public static KeyQuery getSchedule(final long id) {
		return QueryBuilder.query(Constants.SCHEDULE_KIND, id);
	}

	public static Batch insertScheduleLevels(final @NonNull Key parent, final @NonNull Schedule schedule) {
		Batch batch = new Batch();

		Key key = Key.builder(Constants.TIME_RANGE_KIND, parent).build();

		int levelNumber = 0;
		for (ScheduleLevel level : schedule.getScheduleLevelsList()) {
			for (TimeRange range : level.getTimeRangesList()) {
				batch.add(QueryBuilder.insert(key)
						.value(Constants.START, range.getStart())
						.value(Constants.LENGTH, range.getLength())
						.value(Constants.USER, range.getUser().getId())
						.value(Constants.LEVEL, levelNumber));
			}

			levelNumber++;
		}

		return batch;
	}

	public static Query getScheduleLeveles(long id) {
		Key parent = Key.builder(Constants.SCHEDULE_KIND, id).build();
		return QueryBuilder.query().kindOf(Constants.TIME_RANGE_KIND).filterBy(QueryBuilder.ancestor(parent));
	}

	public static MutationStatement deleteScheduleLevel(@NonNull final Key key) {
		return QueryBuilder.delete(key);
	}

	public static Key getScheduleKey(long id) {
		return Key.builder(Constants.SCHEDULE_KIND, id).build();
	}

	public static Query getScheduleLevelsForUser(long id) {
		return QueryBuilder.query()
				.kindOf(Constants.TIME_RANGE_KIND)
				.filterBy(eq(Constants.USER, id));
	}

	public static Query getSchedulesForTeam(long id) {
		return QueryBuilder.query()
				.kindOf(Constants.SCHEDULE_KIND)
				.filterBy(eq(Constants.TEAM, id));
	}

	public static KeyQuery getTeamKey(long id) {
		return QueryBuilder.query(Constants.SCHEDULE_KIND, id);
	}

}
