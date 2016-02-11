package io.vigilante.site.impl.datastore.basic.user;

import static com.spotify.asyncdatastoreclient.QueryBuilder.asc;
import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.User.NotificationRule;
import io.vigilante.site.impl.datastore.basic.Constants;
import lombok.NonNull;

import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.Update;

public class UserQueries {

	public static Query getUsers() {
		return QueryBuilder.query().kindOf(Constants.USER_KIND).orderBy(asc(Constants.NAME));
	}

	public static Insert insertUser(final User user) {
		return QueryBuilder.insert(Constants.USER_KIND)
				.value(Constants.NAME, user.getName())
				.value(Constants.EMAIL, user.getEmail())
				.value(Constants.TIME_ZONE, user.getTimeZone());
	}

	public static Update updateUser(final long id, final User user) {
		return QueryBuilder.update(Constants.USER_KIND, id)
				.value(Constants.NAME, user.getName())
				.value(Constants.EMAIL, user.getEmail())
				.value(Constants.TIME_ZONE, user.getTimeZone());
	}

	public static Delete deleteUser(final long id) {
		return QueryBuilder.delete(Constants.USER_KIND, id);
	}

	public static Insert addNotification(final long userId, @NonNull final NotificationRule notification) {
		Key parent = Key.builder(Constants.USER_KIND, userId).build();
		Key key = Key.builder(Constants.NOTIFICATION_KIND, parent).build();

		Insert insert = QueryBuilder.insert(key).value(Constants.USER_ID, userId)
				.value(Constants.START_OFFSET, notification.getStart())
				.value(Constants.TYPE, notification.getType().getNumber())
				.value(Constants.ADDRESS, notification.getContactAddress())
				.value(Constants.ALIAS, notification.getContactAlias());

		if (notification.hasContactDetails()) {
			insert.value(Constants.DETAILS, notification.getContactDetails());
		}

		return insert;
	}

	public static Update modifyNotification(final long userId, final long notificationId,
			@NonNull NotificationRule notification) {
		Update update = QueryBuilder.update(notificationKey(userId, notificationId))
				.value(Constants.USER_ID, userId)
				.value(Constants.START_OFFSET, notification.getStart())
				.value(Constants.TYPE, notification.getType().getNumber())
				.value(Constants.ADDRESS, notification.getContactAddress())
				.value(Constants.ALIAS, notification.getContactAlias());

		if (notification.hasContactDetails()) {
			update.value(Constants.DETAILS, notification.getContactDetails());
		}

		return update;
	}

	public static Delete deleteNotification(final long userId, final long notificationId) {
		return QueryBuilder.delete(notificationKey(userId, notificationId));
	}

	public static KeyQuery getUser(final long id) {
		return QueryBuilder.query(Constants.USER_KIND, id);
	}

	public static Query getNotifications(final long id) {
		Key parent = Key.builder(Constants.USER_KIND, id).build();

		return QueryBuilder.query().kindOf(Constants.NOTIFICATION_KIND).filterBy(QueryBuilder.ancestor(parent));
	}

	public static KeyQuery getUserKey(final long id) {
		return QueryBuilder.query(Key.builder(Constants.USER_KIND, id).build());
	}

	private static Key notificationKey(final Long userId, final Long notificationId) {
		Key parent = Key.builder(Constants.USER_KIND, userId).build();
		Key key = Key.builder(Constants.NOTIFICATION_KIND, notificationId, parent).build();
		return key;
	}

}
