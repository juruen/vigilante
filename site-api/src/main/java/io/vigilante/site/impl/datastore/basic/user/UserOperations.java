package io.vigilante.site.impl.datastore.basic.user;

import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.User.NotificationRule;
import io.vigilante.UserProtos.Users;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.NonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryResult;

public class UserOperations {

	private final Datastore datastore;

	public UserOperations(final Datastore datastore) {
		this.datastore = datastore;
	}

	public ListenableFuture<Users> getUsers(final @NonNull String namespace) {
		return Futures.transform(datastore.executeAsync(UserQueries.getUsers()), buildUsers());
	}

	public ListenableFuture<Long> addUser(final @NonNull String namespace, final @NonNull User user) {
		ListenableFuture<MutationResult> insert = Futures.transform(verifyUser(namespace, user),
				mutateUser(UserQueries.insertUser(user)));

		ListenableFuture<Long> id = Futures.transform(insert, AsyncUtil.fetchId());

		return Futures.transform(id, addNotifications(namespace, user));
	}

	@SuppressWarnings("unchecked")
	public ListenableFuture<Void> updateUser(final @NonNull String namespace, final long id, final @NonNull User user) {
		ListenableFuture<QueryResult> userNotifications = datastore.executeAsync(UserQueries.getNotifications(id));
		ListenableFuture<Batch> deleteNotificationsBatch = Futures.transform(userNotifications,
				AsyncUtil.buildDeleteBatch());
		ListenableFuture<User> verify = verifyUser(namespace, user);

		return Futures.transform(Futures.allAsList(verify, deleteNotificationsBatch), updateUser(id));
	}

	public ListenableFuture<Void> deleteUser(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(UserQueries.deleteUser(id)), AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Long> addNotification(final @NonNull String namespace, final long userId,
			@NonNull final NotificationRule notification) {
		return Futures.transform(datastore.executeAsync(UserQueries.addNotification(userId, notification)),
				AsyncUtil.fetchId());
	}

	public ListenableFuture<Void> updateNotification(final @NonNull String namespace, final long userId,
			final long notificationId, @NonNull final NotificationRule notification) {
		return Futures.transform(
				datastore.executeAsync(UserQueries.modifyNotification(userId, notificationId, notification)),
				AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Void> deleteNotification(final @NonNull String namespace, final long userId,
			final long notificationId) {
		return Futures.transform(datastore.executeAsync(UserQueries.deleteNotification(userId, notificationId)),
				AsyncUtil.emptyResponse());
	}

	public ListenableFuture<User> getUser(final @NonNull String namespace, final long id) {
		ListenableFuture<User> user = Futures.transform(datastore.executeAsync(UserQueries.getUser(id)), buildUser());
		ListenableFuture<List<Entity>> notifications = Futures.transform(
				datastore.executeAsync(UserQueries.getNotifications(id)), buildNotifications());

		return Futures.transform(Futures.successfulAsList(Arrays.asList(user, notifications)), buildCompleteUser());
	}

	public ListenableFuture<Void> userExists(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(UserQueries.getUserKey(id)), AsyncUtil.keyExists("User"));
	}

	public ListenableFuture<Void> usersExist(final @NonNull String namespace, final List<User> users) {
		List<ListenableFuture<Void>> usersFutures = new ArrayList<>();
		for (User user : users) {
			usersFutures.add(userExists(namespace, user.getId()));
		}

		return Futures.transform(Futures.allAsList(usersFutures), AsyncUtil.asListCheck());
	}

	private AsyncFunction<List<Object>, User> buildCompleteUser() {
		return new AsyncFunction<List<Object>, User>() {

			@Override
			public ListenableFuture<User> apply(List<Object> result) throws Exception {
				if (result.get(0) == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("user doesn't exist"));
				}

				User user = (User) result.get(0);

				if (result.get(1) == null) {
					return Futures.immediateFuture(user);
				}

				@SuppressWarnings("unchecked")
				List<Entity> notifications = (List<Entity>) result.get(1);

				return Futures.immediateFuture(UserUtil.buildCompleteUser(user, notifications));
			}
		};
	}

	private AsyncFunction<QueryResult, List<Entity>> buildNotifications() {
		return new AsyncFunction<QueryResult, List<Entity>>() {

			@Override
			public ListenableFuture<List<Entity>> apply(QueryResult result) throws Exception {
				return Futures.immediateFuture(result.getAll());

			}
		};
	}

	private ListenableFuture<User> verifyUser(final @NonNull String namespace, final @NonNull User user) {
		if (!user.hasName()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Name is missing"));
		}

		if (!user.hasEmail()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Email is missing"));
		}

		if (!user.hasTimeZone()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Time zone is missing"));
		}

		return Futures.immediateFuture(user);
	}

	private AsyncFunction<QueryResult, Users> buildUsers() {
		return new AsyncFunction<QueryResult, Users>() {

			@Override
			public ListenableFuture<Users> apply(QueryResult result) throws Exception {
				List<User> users = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						users.add(UserUtil.buildBasicUser(e));
					}
				}

				Users.Builder builder = Users.newBuilder();

				if (!users.isEmpty()) {
					builder.addAllUsers(users);
				}

				return Futures.immediateFuture(builder.build());
			}
		};
	}

	private AsyncFunction<User, MutationResult> mutateUser(final MutationStatement mutation) {
		return new AsyncFunction<User, MutationResult>() {

			@Override
			public ListenableFuture<MutationResult> apply(User user) throws Exception {
				return datastore.executeAsync(mutation);
			}
		};
	}

	private AsyncFunction<QueryResult, User> buildUser() {
		return new AsyncFunction<QueryResult, User>() {

			@Override
			public ListenableFuture<User> apply(QueryResult result) throws Exception {
				Entity e = result.getEntity();

				if (e == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("User doesn't exist"));
				}

				return Futures.immediateFuture(UserUtil.buildBasicUser(e));
			}

		};
	}

	private AsyncFunction<Long, Long> addNotifications(final String namespace, final User user) {
		return new AsyncFunction<Long, Long>() {

			@Override
			public ListenableFuture<Long> apply(Long id) throws Exception {
				Batch batch = new Batch();

				for (NotificationRule notification : user.getNotificationsList()) {
					batch.add(UserQueries.addNotification(id, notification));
				}

				return Futures.transform(datastore.executeAsync(batch), AsyncUtil.returnId(id));
			}
		};
	}

	private AsyncFunction<List<Object>, Void> updateUser(final long id) {
		return new AsyncFunction<List<Object>, Void>() {

			@Override
			public ListenableFuture<Void> apply(List<Object> input) throws Exception {
				User user = (User) input.get(0);
				Batch batch = (Batch) input.get(1);

				for (NotificationRule notification : user.getNotificationsList()) {
					batch.add(UserQueries.addNotification(id, notification));
				}

				batch.add(UserQueries.updateUser(id, user));

				return Futures.transform(datastore.executeAsync(batch), AsyncUtil.emptyResponse());
			}
		};
	}
}