package io.vigilante.site.api.impl.datastoreng;


import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import com.spotify.asyncdatastoreclient.Value;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.impl.datastore.basic.Constants;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Common {
    public static final int MAX_CONCURRENT_TRANSACTIONS = 24;

    public static CompletableFuture<Void> deleteEntity(final @NonNull String namespace,
                                                       final String kind,
                                                       final String what,
                                                       final long id,
                                                       final DatastoreWrapper datastore) {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        final Key key = Key.builder(kind, id).build();

        return datastore
            .exec(QueryBuilder.query(key), txn)
            .thenApply(Common::entityOrElseThrow)
            .thenAccept(
                e -> {
                    if (e.getInteger(Constants.REF_COUNT) != 0) {
                        throw new ReferentialIntegrityException(what + " is being used");
                    }
                })
            .thenCompose(ignored -> datastore.exec(QueryBuilder.delete(key), txn))
            .thenAccept(ignored -> {
            });
    }


    public static CompletableFuture<Batch> increaseRefCounter(
        final @NonNull String namespace,
        final DatastoreWrapper datastore,
        final String kind,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn,
        final long add) {

        final Key key = Key.builder(kind, id).build();

        return datastore
            .exec(QueryBuilder.query(key), txn)
            .thenApply(r ->
            {
                final long refCounter = r.getEntity().getInteger(Constants.REF_COUNT) + add;
                final Entity newEntity = Entity
                    .builder(r.getEntity())
                    .property(Constants.REF_COUNT, refCounter)
                    .build();

                return QueryBuilder.batch().add(QueryBuilder.update(newEntity));
            });
    }

    public static List<Value> getList(Entity entity, String property) {
        try {
            return Optional.ofNullable(entity.getList(property)).orElse(ImmutableList.of());
        } catch (IllegalArgumentException e) {
            return ImmutableList.of();
        }
    }

    public static Entity entityOrElseThrow(QueryResult queryResult) {
        if (queryResult.getAll().size() > 0 ) {
            return queryResult.getEntity();
        } else {
            throw new SiteExternalException("entity doesn't exist");
        }
    }

    public static List<Object> toList(List<?> users) {
        return Optional.ofNullable(users)
            .flatMap(us ->
                Optional.of(us.stream().map(e -> (Object) e).collect(Collectors.toList())))
            .orElse(ImmutableList.of());
    }

    public static List<Long> getIds(Entity entity, String property) {
        return getList(entity, property)
            .stream()
            .map(Value::getInteger)
            .collect(Collectors.toList());
    }

    public static List<String> getStringList(Entity entity, String property) {
        return Common.getList(entity, property)
            .stream()
            .map(Value::getString)
            .collect(Collectors.toList());
    }

    public static Map<String, String> getStringMap(Entity entity, String property) {
        return Optional
            .ofNullable(entity.getEntity(property))
            .orElse(Entity.builder().build())
            .getProperties()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getString()));
    }

    public static Entity toEntity(Map<String, String> map) {
        final Entity.Builder entity = Entity.builder();
        map.entrySet().stream().forEach(kv -> entity.property(kv.getKey(), kv.getValue()));

        return entity.build();
    }

    public static boolean hasSameName(Entity entity, String name) {
        return entity.getString(Constants.NAME).equals(name);
    }

    public static boolean hasSameName(Entity a, Entity b) {
        return hasSameName(a, b.getString(Constants.NAME));
    }
}
