package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Value;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.User;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DatastoreUserManager {
    private final static EntityDescriptor DESCRIPTOR = EntityDescriptor.USER;

    private final DatastoreHandler datastore;

    public DatastoreUserManager(DatastoreHandler datastore) {
        this.datastore = datastore;
    }

    public CompletableFuture<List<User>> getUsers(final @NonNull String namespace) {
        return datastore.fetchEntities(namespace, DESCRIPTOR, this::entityToUser);
    }

    public CompletableFuture<String> addUser(
        final @NonNull String namespace, final @NonNull User user
    ) {
        return datastore.addEntity(namespace, DESCRIPTOR, userToEntity(user));
    }

    public CompletableFuture<Void> updateUser(
        final @NonNull String namespace, final String id, final @NonNull User user)
    {
        return datastore.updateEntity(namespace, DESCRIPTOR, id, userToEntity(user));
    }

    public CompletableFuture<Void> deleteUser(
        final @NonNull String namespace, final String id)
    {
        return datastore.deleteEntity(namespace, DESCRIPTOR, id);
    }

    public CompletableFuture<User> getUser(
        final @NonNull String namespace,
        final String id)
    {
        return datastore.fetchEntity(namespace, DESCRIPTOR, id, this::entityToUser);
    }

    private Entity notificationToEntity(User.NotificationRule notificationRule) {
        final Entity.Builder entity = Entity.builder();

        entity.property(Constants.ADDRESS, notificationRule.getContactAddress())
            .property(Constants.ALIAS, notificationRule.getContactAlias())
            .property(Constants.START, notificationRule.getStart())
            .property(Constants.TYPE, notificationRule.getType().toString());

        notificationRule.getContactDetails().ifPresent(d -> entity.property(Constants.DETAILS, d));

        return entity.build();
    }

    private Entity userToEntity(@NonNull User user) {
        final Entity.Builder entity = Entity.builder(Constants.USER_KIND)
            .property(Constants.NAME, user.getName())
            .property(Constants.EMAIL, user.getEmail())
            .property(Constants.TIME_ZONE, user.getTimeZone());

        if (!user.getNotifications().isEmpty()) {
            final List<Object> notifications = user.getNotifications().stream()
                .map(this::notificationToEntity)
                .collect(Collectors.toList());

            entity.property(Constants.NOTIFICATION_RULES, notifications);
        }

        return entity.build();
    }

    private User entityToUser(Entity entity) {
        return User.builder()
            .id(Optional.of(entity.getKey().getName()))
            .email(entity.getString(Constants.EMAIL))
            .name(entity.getString(Constants.NAME))
            .timeZone(entity.getString(Constants.TIME_ZONE))
            .notifications(notificationsToEntity(entity))
            .build();
    }

    private List<User.NotificationRule> notificationsToEntity(Entity entity) {
        if (entity.contains(Constants.NOTIFICATION_RULES)) {
            final List<Value> notifications = entity.getList(Constants.NOTIFICATION_RULES);
            return notifications.stream()
                .map(this::notificationToEntity)
                .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }

    private User.NotificationRule notificationToEntity(Value value) {
        final Entity entity = value.getEntity();

        final User.NotificationRule.NotificationRuleBuilder rule = User.NotificationRule.builder()
            .contactAddress(entity.getString(Constants.ADDRESS))
            .contactAlias(entity.getString(Constants.ALIAS))
            .start(entity.getInteger(Constants.START))
            .contactDetails(Optional.ofNullable(entity.getString(Constants.DETAILS)))
            .type(User.NotificationRule.NotificationType.valueOf(entity.getString(Constants.TYPE)));

        return rule.build();
    }
}
