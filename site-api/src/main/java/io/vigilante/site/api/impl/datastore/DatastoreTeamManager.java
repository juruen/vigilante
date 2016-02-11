package io.vigilante.site.api.impl.datastore;

import io.vigilante.ScheduleProtos.Schedules;
import io.vigilante.ServiceProtos.Services;
import io.vigilante.TeamProtos.Team;
import io.vigilante.TeamProtos.Teams;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.api.impl.datastore.basic.schedule.ScheduleOperations;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;
import io.vigilante.site.impl.datastore.basic.service.ServiceOperations;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;
import io.vigilante.site.impl.datastore.basic.user.UserOperations;

import java.util.List;

import lombok.NonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessage;
import com.spotify.asyncdatastoreclient.Datastore;

public class DatastoreTeamManager implements TeamManager {

	private final TeamOperations teamOperations;
	private final UserOperations userOperations;
	private final ScheduleOperations scheduleOperations;
	private final ServiceOperations serviceOperations;

	public DatastoreTeamManager(final Datastore datastore) {
		this.teamOperations = new TeamOperations(datastore);
		this.userOperations = new UserOperations(datastore);
		this.scheduleOperations = new ScheduleOperations(datastore);
		this.serviceOperations = new ServiceOperations(datastore);

	}

	@Override
	public ListenableFuture<Team> getTeam(final @NonNull String namespace, long id) {
		return Futures.transform(teamOperations.getTeam(namespace, id),
				AsyncUtil.filterNonExistingUsers(namespace, userOperations));
	}

	@Override
	public ListenableFuture<Teams> getTeams(final @NonNull String namespace) {
		return teamOperations.getTeams(namespace);
	}

	@Override
	public ListenableFuture<Long> addTeam(final @NonNull String namespace, final @NonNull Team team) {
		ListenableFuture<Long> addTeam = teamOperations.addTeam(namespace, team);
		ListenableFuture<Void> usersExist = userOperations.usersExist(namespace, team.getUsersList());

		return AsyncUtil.conditionalAdd(addTeam, usersExist);
	}

	@Override
	public ListenableFuture<Void> updateTeam(final @NonNull String namespace, long id, final @NonNull Team team) {
		ListenableFuture<Void> updateTeam = teamOperations.modifyTeam(namespace, id, team);
		ListenableFuture<Void> usersExist = userOperations.usersExist(namespace, team.getUsersList());

		return AsyncUtil.conditionalUpdate(updateTeam, usersExist);
	}

	@Override
	public ListenableFuture<Void> deleteTeam(final @NonNull String namespace, long id) {
		ListenableFuture<List<GeneratedMessage>> checks = Futures.allAsList(ImmutableList.of(
				scheduleOperations.getSchedulesForTeam(namespace, id),
				serviceOperations.getServicesForTeam(namespace, id)));

		return Futures.transform(checks, conditionalDeleteTeam(namespace, id));
	}

	@Override
	public ListenableFuture<Teams> getTeamsForUser(final @NonNull String namespace, long id) {
		return teamOperations.getTeamsForUser(namespace, id);
	}

	private AsyncFunction<List<GeneratedMessage>, Void> conditionalDeleteTeam(final @NonNull String namespace,
			final long id) {
		return new AsyncFunction<List<GeneratedMessage>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<GeneratedMessage> results) throws Exception {

				if (results.get(0) != null && ((Schedules) results.get(0)).getSchedulesCount() > 0) {
					return Futures.immediateFailedFuture(new ReferentialIntegrityException(
							"Team belongs to a schedule(s)"));
				}

				if (results.get(1) != null && ((Services) results.get(1)).getServicesCount() > 0) {
					return Futures.immediateFailedFuture(new ReferentialIntegrityException(
							"Team belongs to a service(s)"));
				}

				return teamOperations.deleteTeam(namespace, id);
			}
		};
	}

}
