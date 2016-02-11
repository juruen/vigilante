package io.vigilante.site.impl.datastore.basic.incident;

import io.vigilante.IncidentProtos.Incident;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.TeamProtos.Team;
import io.vigilante.site.impl.datastore.basic.Constants;

import com.spotify.asyncdatastoreclient.Entity;

public class IncidentUtil {

	public static Incident buildBasicIncident(Entity e) {
		return Incident.newBuilder()
				.setId(e.getKey().getName())
				.setDescription(e.getString(Constants.DESCRIPTION))
				.setStart(e.getInteger(Constants.START))
				.setService(Service.newBuilder().setId(e.getInteger(Constants.SERVICE)))
				.setTeam(Team.newBuilder().setId(e.getInteger(Constants.TEAM)))
				.setState(Incident.StateType.valueOf(e.getInteger(Constants.STATE).intValue()))
				.build();
	}

	public static Incident buildBasicOpenIncident(Entity e) {
		return Incident.newBuilder()
				.setId(e.getString(Constants.INCIDENT_ID))
				.setDescription(e.getString(Constants.DESCRIPTION))
				.setKey(e.getString(Constants.INCIDENT_KEY))
				.setStart(e.getInteger(Constants.CREATION_TIME))
				.setService(Service.newBuilder().setId(e.getInteger(Constants.SERVICE_ID)))
				.setTeam(Team.newBuilder().setId(e.getInteger(Constants.TEAM_ID)))
				.setState(Incident.StateType.valueOf(e.getString(Constants.STATE)))
				.build();
	}
}