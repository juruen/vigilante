package io.vigilante.site.api.impl.datastore;

import io.vigilante.ScheduleProtos.Schedules;
import io.vigilante.TeamProtos.Teams;
import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.Users;
import io.vigilante.UserProtos.User.NotificationRule;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.api.impl.datastore.basic.schedule.ScheduleOperations;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;
import io.vigilante.site.impl.datastore.basic.user.UserOperations;

import java.util.Arrays;
import java.util.List;

import lombok.NonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessage;
import com.spotify.asyncdatastoreclient.Datastore;

public class DatastoreUserManager implements UserManager {

	private final UserOperations userOperations;
	private final TeamOperations teamOperations;
	private final ScheduleOperations scheduleOperations;

	public DatastoreUserManager(final Datastore datastore) {
		this.userOperations = new UserOperations(datastore);
		this.teamOperations = new TeamOperations(datastore);
		this.scheduleOperations = new ScheduleOperations(datastore);
	}

	@Override
	public ListenableFuture<Users> getUsers(final @NonNull String namespace) {
		return userOperations.getUsers(namespace);
	}

	@Override
	public ListenableFuture<Long> addUser(final @NonNull String namespace, final @NonNull User user) {
		return userOperations.addUser(namespace, user);
	}

	@Override
	public ListenableFuture<Void> updateUser(final @NonNull String namespace, final long id, final @NonNull User user) {
		return userOperations.updateUser(namespace, id, user);
	}

	@Override
	public ListenableFuture<Void> deleteUser(final @NonNull String namespace, final long id) {
		ListenableFuture<List<GeneratedMessage>> references = Futures.successfulAsList(Arrays.asList(
				teamOperations.getTeamsForUser(namespace, id),
				scheduleOperations.getSchedulesForUser(namespace, id)));

		return Futures.transform(references, conditionalDeleteUser(namespace, id));
	}

	@Override
	public ListenableFuture<Long> addUserNotification(final @NonNull String namespace, final long userId,
			final @NonNull NotificationRule notification) {
		return userOperations.addNotification(namespace, userId, notification);
	}

	@Override
	public ListenableFuture<Void> updateUserNotification(final @NonNull String namespace, final long userId,
			final long notificationId, final @NonNull NotificationRule notification) {
		return userOperations.updateNotification(namespace, userId, notificationId, notification);

	}

	@Override
	public ListenableFuture<Void> deleteUserNotification(final @NonNull String namespace, final long userId,
			final long notificationId) {
		return userOperations.deleteNotification(namespace, userId, notificationId);
	}

	@Override
	public ListenableFuture<User> getUser(final @NonNull String namespace, final long id) {
		return userOperations.getUser(namespace, id);
	}

	private AsyncFunction<List<GeneratedMessage>, Void> conditionalDeleteUser(final @NonNull String namespace,
			final long id) {
		return new AsyncFunction<List<GeneratedMessage>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<GeneratedMessage> results) throws Exception {

				Teams teams = (Teams) results.get(0);
				Schedules schedules = (Schedules) results.get(1);

				if (teams.getTeamsCount() > 0) {
					return Futures
							.immediateFailedFuture(new ReferentialIntegrityException("User belongs to a team(s)"));
				}

				if (schedules.getSchedulesCount() > 0) {
					return Futures.immediateFailedFuture(new ReferentialIntegrityException(
							"User belongs to a schedule(s)"));
				}

				return userOperations.deleteUser(namespace, id);
			}
		};
	}
}
