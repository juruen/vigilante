package io.vigilante.site.api.impl.datastore.basic.schedule;

import io.vigilante.ScheduleProtos;
import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedule.TimeRange;
import lombok.Data;
import lombok.Getter;

@Data
public class TimeRangeLevel {
	@Getter
	private final TimeRange timeRange;
	@Getter
	private final int level;
}