package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import com.spotify.asyncdatastoreclient.Value;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.User;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DatastoreUserManager {

    private final DatastoreWrapper datastore;

    public DatastoreUserManager(final DatastoreWrapper datastore) {
        this.datastore = datastore;
    }

    public CompletableFuture<List<User>> getUsers(final @NonNull String namespace) {
        return datastore
            .exec(QueryBuilder.query().kindOf(Constants.USER_KIND))
            .thenApply(this::buildUserList);
    }

    public CompletableFuture<Long> addUser(final @NonNull String namespace,
                                           final @NonNull User user) {
        return datastore
            .exec(QueryBuilder.insert(buildUserEntity(user, 0)))
            .thenApply(r -> r.getInsertKey().getId());
    }

    public CompletableFuture<Void> updateUser(final @NonNull String namespace,
                                              final long id,
                                              final @NonNull User user) {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        return datastore
            .exec(QueryBuilder.query(Constants.USER_KIND, id), txn)
            .thenCompose(r -> updateUserSameRefCount(user, txn, r));
    }

    public CompletableFuture<Void> deleteUser(final @NonNull String namespace, final long id) {
        return Common.deleteEntity(namespace, Constants.USER_KIND, "user", id, datastore);
    }

    public CompletableFuture<User> getUser(final @NonNull String namespace, final long id) {
        return datastore
            .exec(QueryBuilder.query(Constants.USER_KIND, id))
            .thenApply(r -> buildUser(r.getEntity()));
    }

    public CompletableFuture<MutationStatement> increaseRefCounter(
        final @NonNull String namespace,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn) {
        return increaseRefCounter(namespace, id, txn, 1);
    }

    public CompletableFuture<MutationStatement> decreaseRefCounter(
        final @NonNull String namespace,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn) {
        return increaseRefCounter(namespace, id, txn, -1);
    }

    private CompletableFuture<MutationStatement> increaseRefCounter(
        final @NonNull String namespace,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn,
        final long add) {

        final Key key = Key.builder(Constants.USER_KIND, id).build();

        return datastore
            .exec(QueryBuilder.query(key), txn)
            .thenApply(r ->
            {
                final long refCounter = r.getEntity().getInteger(Constants.REF_COUNT) + add;
                final Entity newEntity = Entity
                    .builder(r.getEntity())
                    .property(Constants.REF_COUNT, refCounter)
                    .build();

                return QueryBuilder.update(newEntity);
            });
    }

    private CompletableFuture<Void> updateUserSameRefCount(User user,
                                                           CompletableFuture<TransactionResult> txn,
                                                           QueryResult r) {
        final long refCount = r.getEntity().getInteger(Constants.REF_COUNT);

        return datastore
            .exec(QueryBuilder.insert(buildUserEntity(user, refCount)), txn)
            .thenAccept(ignored -> {
            });
    }

    private Entity buildNotificationEntity(User.NotificationRule notificationRule) {
        final Entity.Builder entity = Entity.builder();

        entity.property(Constants.ADDRESS, notificationRule.getContactAddress())
            .property(Constants.ALIAS, notificationRule.getContactAlias())
            .property(Constants.START, notificationRule.getStart())
            .property(Constants.TYPE, notificationRule.getType().toString());

        notificationRule.getContactDetails().ifPresent(d -> entity.property(Constants.DETAILS, d));

        return entity.build();
    }

    private Entity buildUserEntity(@NonNull User user, long refCount) {
        final Entity.Builder entity = Entity.builder(Constants.USER_KIND)
            .property(Constants.NAME, user.getName())
            .property(Constants.EMAIL, user.getEmail())
            .property(Constants.REF_COUNT, refCount);

        if (!user.getNotifications().isEmpty()) {
            final List<Entity> notifications = user.getNotifications().stream()
                .map(this::buildNotificationEntity)
                .collect(Collectors.toList());

            entity.property(Constants.NOTIFICATION_RULES, notifications);
        }

        return entity.build();
    }

    private List<User> buildUserList(QueryResult result) {
        return result.getAll().stream()
            .map(this::buildUser)
            .collect(Collectors.toList());
    }

    private User buildUser(Entity entity) {
        return User.builder()
            .id(Optional.of(entity.getKey().getId()))
            .email(entity.getString(Constants.EMAIL))
            .name(entity.getString(Constants.NAME))
            .timeZone(entity.getString(Constants.TIME_ZONE))
            .notifications(buildUserNotifications(entity))
            .build();
    }

    private List<User.NotificationRule> buildUserNotifications(Entity entity) {
        if (entity.contains(Constants.NOTIFICATION_RULES)) {
            final List<Value> notifications = entity.getList(Constants.NOTIFICATION_RULES);
            return notifications.stream()
                .map(this::buildUserNotification)
                .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }

    private User.NotificationRule buildUserNotification(Value value) {
        final Entity entity = value.getEntity();

        final User.NotificationRule.NotificationRuleBuilder rule = User.NotificationRule.builder()
            .contactAddress(entity.getString(Constants.ADDRESS))
            .contactAlias(entity.getString(Constants.ALIAS))
            .start(entity.getInteger(Constants.START))
            .type(User.NotificationRule.NotificationType.valueOf(entity.getString(Constants.TYPE)));

        if (entity.contains(Constants.DETAILS)) {
            rule.contactDetails(Optional.of(entity.getString(Constants.DETAILS)));
        }

        return rule.build();
    }
}
