package io.vigilante.site.api;

import io.vigilante.TeamProtos.Team;
import io.vigilante.TeamProtos.Teams;

import com.google.common.util.concurrent.ListenableFuture;

public interface TeamManager {

	ListenableFuture<Team> getTeam(final String namespace, final long id);

	ListenableFuture<Teams> getTeams(final String namespace);

	ListenableFuture<Long> addTeam(final String namespace, final Team team);

	ListenableFuture<Void> updateTeam(final String namespace, final long id, final Team team);

	ListenableFuture<Void> deleteTeam(final String namespace, final long id);

	ListenableFuture<Teams> getTeamsForUser(final String namespace, final long id);
}
