package io.vigilante.site.api.impl.datastore.basic.schedule;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.TeamProtos.Team;
import io.vigilante.site.impl.datastore.basic.Constants;

import com.spotify.asyncdatastoreclient.Entity;

public class ScheduleUtil {

	public static Schedule buildBasicSchedule(Entity e) {
		return Schedule.newBuilder()
				.setId(e.getKey().getId())
				.setName(e.getString(Constants.NAME))
				.setStart(e.getInteger(Constants.START))
				.setLength(e.getInteger(Constants.LENGTH))
				.setTeam(Team.newBuilder().setId(e.getInteger(Constants.TEAM)))
				.build();
	}

}
