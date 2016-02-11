package io.vigilante.site.api;

import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.Users;
import io.vigilante.UserProtos.User.NotificationRule;

import com.google.common.util.concurrent.ListenableFuture;

public interface UserManager {

	ListenableFuture<User> getUser(final String namespace, final long id);

	ListenableFuture<Users> getUsers(final String namespace);

	ListenableFuture<Long> addUser(final String namespace, final User user);

	ListenableFuture<Void> updateUser(final String namespace, final long id, final User user);

	ListenableFuture<Void> deleteUser(final String namespace, final long id);

	ListenableFuture<Long> addUserNotification(final String namespace, final long userId,
											   final NotificationRule notification);

	ListenableFuture<Void> updateUserNotification(final String namespace, final long userId, final long notificationId,
												  final NotificationRule notification);

	ListenableFuture<Void> deleteUserNotification(final String namespace, final long userId, final long notificationId);
}
