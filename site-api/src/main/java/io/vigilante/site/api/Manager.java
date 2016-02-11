package io.vigilante.site.api;

public interface Manager {

	UserManager getUserManager();

	TeamManager getTeamManager();

	ScheduleManager getScheduleManager();

	ServiceManager getServiceManager();

	IncidentManager getIncidentManager();

	AuthenticationManager getAuthenticationManager();
}
