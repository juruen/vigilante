package io.vigilante.site.api.impl.datastore.util;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.ResponseCallback;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.impl.datastore.basic.user.UserOperations;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;

@Slf4j
public class AsyncUtil {

	private static Team UNDEFINED_TEAM = Team.newBuilder().setId(1).setName("Undefined team").build();
	private static Schedule UNDEFINED_SCHEDULE = Schedule.newBuilder().setId(1).setName("Undefined schedule")
			.setLength(0).setStart(0).build();

	public static <T> void addCallback(final ListenableFuture<T> future, final ResponseCallback<T> callback,
			final String what) {
		Futures.addCallback(future, new FutureCallback<T>() {

			@Override
			public void onSuccess(T result) {
				callback.onFinished(true, null, result);
			}

			@Override
			public void onFailure(Throwable t) {
				log.error("failed to {}", what);

				callback.onFinished(false, t, null);
			}
		});
	}

	public static AsyncFunction<Team, Team> filterNonExistingUsers(final @NonNull String namespace,
			final @NonNull UserOperations userOperations) {
		return new AsyncFunction<Team, Team>() {

			@Override
			public ListenableFuture<Team> apply(final Team team) throws Exception {
				List<ListenableFuture<User>> users = new ArrayList<>();

				for (User user : team.getUsersList()) {
					users.add(userOperations.getUser(namespace, user.getId()));
				}

				return Futures.transform(Futures.successfulAsList(users), addExistingUsers(team));
			}
		};
	}

	public static AsyncFunction<MutationResult, Long> fetchId() {
		return new AsyncFunction<MutationResult, Long>() {

			@Override
			public ListenableFuture<Long> apply(MutationResult result) throws Exception {
				return Futures.immediateFuture(result.getInsertKey().getId());
			}
		};
	}

	public static AsyncFunction<MutationResult, Key> fetchInsertKey() {
		return new AsyncFunction<MutationResult, Key>() {

			@Override
			public ListenableFuture<Key> apply(MutationResult result) throws Exception {
				Key key = result.getInsertKey();

				if (key == null) {
					throw new IllegalArgumentException("Inserted key is null");
				}

				return Futures.immediateFuture(key);
			}
		};
	}

	public static AsyncFunction<MutationResult, Void> emptyResponse() {
		return new AsyncFunction<MutationResult, Void>() {

			@Override
			public ListenableFuture<Void> apply(MutationResult result) throws Exception {
				return Futures.immediateFuture(null);
			}
		};
	}

	public static AsyncFunction<MutationResult, Long> returnId(final Long id) {
		return new AsyncFunction<MutationResult, Long>() {

			@Override
			public ListenableFuture<Long> apply(final MutationResult input) throws Exception {
				return Futures.immediateFuture(id);
			}
		};
	}

	public static AsyncFunction<QueryResult, Batch> buildDeleteBatch() {
		return new AsyncFunction<QueryResult, Batch>() {

			@Override
			public ListenableFuture<Batch> apply(QueryResult result) throws Exception {
				Batch batch = new Batch();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						batch.add(QueryBuilder.delete(e.getKey()));
					}
				}

				return Futures.immediateFuture(batch);
			}
		};
	}

	public static AsyncFunction<QueryResult, Void> keyExists(final String what) {
		return new AsyncFunction<QueryResult, Void>() {

			@Override
			public ListenableFuture<Void> apply(QueryResult input) throws Exception {
				if (input.getEntity() == null) {
					return Futures.immediateFailedFuture(new ReferentialIntegrityException(String.format(
							"%s doesn't exist", what)));
				} else {
					return Futures.immediateFuture(null);
				}
			}
		};
	}

	@SafeVarargs
	public static ListenableFuture<Long> conditionalAdd(final ListenableFuture<Long> add,
			final ListenableFuture<Void>... checks) {

		if (checks.length == 0) {
			return add;
		}

		return Futures.transform(Futures.allAsList(checks), new AsyncFunction<List<Void>, Long>() {

			@Override
			public ListenableFuture<Long> apply(List<Void> ignored) throws Exception {
				return add;
			}
		});
	}

	@SafeVarargs
	public static ListenableFuture<Void> conditionalUpdate(final ListenableFuture<Void> update,
			final ListenableFuture<Void>... checks) {

		if (checks.length == 0) {
			return update;
		}

		return Futures.transform(Futures.allAsList(checks), new AsyncFunction<List<Void>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<Void> ignored) throws Exception {
				return update;
			}
		});
	}

	public static AsyncFunction<List<Void>, Void> asListCheck() {
		return new AsyncFunction<List<Void>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<Void> input) throws Exception {
				return Futures.immediateFuture(null);
			}
		};
	}

	public static FutureFallback<Team> teamFallback() {
		return new FutureFallback<Team>() {

			@Override
			public ListenableFuture<Team> create(Throwable t) throws Exception {
				return Futures.immediateFuture(UNDEFINED_TEAM);
			}
		};
	}

	public static FutureFallback<Schedule> scheduleFallback() {
		return new FutureFallback<Schedule>() {

			@Override
			public ListenableFuture<Schedule> create(Throwable t) throws Exception {
				return Futures.immediateFuture(UNDEFINED_SCHEDULE);
			}
		};
	}

	private static AsyncFunction<List<User>, Team> addExistingUsers(final Team team) {
		return new AsyncFunction<List<User>, Team>() {

			@Override
			public ListenableFuture<Team> apply(List<User> result) throws Exception {
				List<User> users = new ArrayList<>();

				for (User user : result) {
					if (user != null) {
						users.add(user.toBuilder().clearNotifications().build());
					}
				}

				return Futures.immediateFuture(team.toBuilder().clearUsers().addAllUsers(users).build());

			}
		};
	}

}
