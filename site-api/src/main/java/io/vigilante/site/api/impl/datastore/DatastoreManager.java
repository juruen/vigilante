package io.vigilante.site.api.impl.datastore;

import io.vigilante.site.api.AuthenticationManager;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.api.Manager;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.api.ServiceManager;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.api.impl.datastore.auth.DatastoreAuthenticationManager;

import com.spotify.asyncdatastoreclient.Datastore;

public class DatastoreManager implements Manager {

	private final Datastore datastore;

	public DatastoreManager(final Datastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public UserManager getUserManager() {
		return new DatastoreUserManager(datastore);
	}

	@Override
	public TeamManager getTeamManager() {
		return new DatastoreTeamManager(datastore);
	}

	@Override
	public ScheduleManager getScheduleManager() {
		return new DatastoreScheduleManager(datastore);
	}

	@Override
	public ServiceManager getServiceManager() {
		return new DatastoreServiceManager(datastore);
	}

	@Override
	public IncidentManager getIncidentManager() {
		return new DatastoreIncidentManager(datastore);
	}

	@Override
	public AuthenticationManager getAuthenticationManager() {
		return new DatastoreAuthenticationManager(datastore);
	}
}
