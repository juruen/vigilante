package io.vigilante.site.impl.datastore.basic.team;

import io.vigilante.TeamProtos.Team;
import io.vigilante.TeamProtos.Teams;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryResult;

public class TeamOperations {

	private final Datastore datastore;

	public TeamOperations(final Datastore datastore) {
		this.datastore = datastore;
	}

	public ListenableFuture<Teams> getTeams(final @NonNull String namespace) {
		return Futures.transform(datastore.executeAsync(TeamQueries.getTeams()), buildTeams());
	}

	public ListenableFuture<Long> addTeam(final @NonNull String namespace, final @NonNull Team Team) {
		ListenableFuture<MutationResult> insert = Futures.transform(verifyTeam(namespace, Team),
				mutateTeam(TeamQueries.insertTeam(Team)));

		return Futures.transform(insert, AsyncUtil.fetchId());
	}

	public ListenableFuture<Void> modifyTeam(final @NonNull String namespace, final long id, final @NonNull Team Team) {
		ListenableFuture<MutationResult> update = Futures.transform(verifyTeam(namespace, Team),
				mutateTeam(TeamQueries.updateTeam(id, Team)));

		return Futures.transform(update, AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Void> deleteTeam(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(TeamQueries.deleteTeam(id)), AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Team> getTeam(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(TeamQueries.getTeam(id)), buildTeam());
	}

	public ListenableFuture<Teams> getTeamsForUser(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(TeamQueries.getTeamsForUser(id)), buildTeams());
	}

	public ListenableFuture<Void> teamExists(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(TeamQueries.getTeamKey(id)), AsyncUtil.keyExists("Team"));
	}

	private ListenableFuture<Team> verifyTeam(final @NonNull String namespace, final @NonNull Team team) {
		if (!team.hasName()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Name is missing"));
		}

		for (User user : team.getUsersList()) {
			if (!user.hasId()) {
				return Futures.immediateFailedFuture(new SiteExternalException("User is missing"));
			}
		}

		return Futures.immediateFuture(team);
	}

	private AsyncFunction<QueryResult, Teams> buildTeams() {
		return new AsyncFunction<QueryResult, Teams>() {

			@Override
			public ListenableFuture<Teams> apply(QueryResult result) throws Exception {
				List<Team> teams = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						teams.add(TeamUtil.buildBasicTeam(e).toBuilder().clearUsers().build());
					}
				}

				Teams.Builder builder = Teams.newBuilder();

				if (!teams.isEmpty()) {
					builder.addAllTeams(teams);
				}

				return Futures.immediateFuture(builder.build());
			}
		};
	}

	private AsyncFunction<Team, MutationResult> mutateTeam(final MutationStatement mutation) {
		return new AsyncFunction<Team, MutationResult>() {

			@Override
			public ListenableFuture<MutationResult> apply(Team Team) throws Exception {
				return datastore.executeAsync(mutation);
			}
		};
	}

	private AsyncFunction<QueryResult, Team> buildTeam() {
		return new AsyncFunction<QueryResult, Team>() {

			@Override
			public ListenableFuture<Team> apply(QueryResult result) throws Exception {
				Entity e = result.getEntity();

				if (e == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Team doesn't exist"));
				}

				return Futures.immediateFuture(TeamUtil.buildBasicTeam(e));
			}

		};
	}
}