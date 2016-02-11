package io.vigilante.site.impl.datastore.basic.user;

import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.User.NotificationRule;
import io.vigilante.UserProtos.User.NotificationType;
import io.vigilante.UserProtos.User.NotificationRule.Builder;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import com.spotify.asyncdatastoreclient.Entity;

public class UserUtil {

	public static User buildBasicUser(@NonNull final Entity userEntity) {
		return User.newBuilder()
				.setId(userEntity.getKey().getId())
				.setName(userEntity.getString(Constants.NAME))
				.setEmail(userEntity.getString(Constants.EMAIL))
				.setTimeZone(userEntity.getString(Constants.TIME_ZONE))
				.build();
	}

	public static User buildCompleteUser(@NonNull final User user, @NonNull final List<Entity> entities) {
		List<NotificationRule> notifications = new ArrayList<>();

		for (Entity e : entities) {

			Builder notification = NotificationRule.newBuilder()
					.setId(e.getKey().getId())
					.setStart(e.getInteger(Constants.START_OFFSET))
					.setContactAddress(e.getString(Constants.ADDRESS))
					.setContactAlias(e.getString(Constants.ALIAS))
					.setType(NotificationType.valueOf(e.getInteger(Constants.TYPE).intValue()));

			String details = e.getString(Constants.DETAILS);

			if (details != null) {
				notification.setContactDetails(details);
			}

			notifications.add(notification.build());
		}

		if (!notifications.isEmpty()) {
			return user.toBuilder().addAllNotifications(notifications).build();
		} else {
			return user;
		}
	}
}
