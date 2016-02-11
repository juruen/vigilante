package io.vigilante.site.impl.datastore.basic.service;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.TeamProtos.Team;
import io.vigilante.site.impl.datastore.basic.Constants;
import lombok.NonNull;

import com.spotify.asyncdatastoreclient.Entity;

public class ServiceUtil {

	public static Service buildBasicService(@NonNull final Entity service) {
		final Team team = Team.newBuilder()
				.setId(service.getInteger(Constants.TEAM))
				.build();

		final Schedule schedule = Schedule.newBuilder()
				.setId(service.getInteger(Constants.SCHEDULE))
				.build();

		return Service.newBuilder()
				.setId(service.getKey().getId())
				.setName(service.getString(Constants.NAME))
				.setKey(service.getString(Constants.API_KEY))
				.setTeam(team)
				.setSchedule(schedule).build();
	}
}
