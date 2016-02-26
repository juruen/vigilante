package io.vigilante.site.api.impl.datastoreng;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.TransactionResult;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatastoreUserOps implements UserIntegrity {
    private final DatastoreUserManager userManager;
    private final DatastoreWrapper datastore;

    public DatastoreUserOps(DatastoreUserManager userManager, DatastoreWrapper datastore) {
        this.userManager = userManager;
        this.datastore = datastore;
    }

    @Override
    public CompletableFuture<Long> inertEntityWithUsers(String namespace,
                                                        Entity addEntity,
                                                        List<Long> newUsers)
    {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        final List<CompletableFuture<MutationStatement>> mutations =
            buildInsertMutations(namespace, addEntity, newUsers, txn);

        return datastore
            .execBatch(namespace, txn, mutations)
            .thenApply(r -> r.getInsertKey().getId());
    }

    @Override
    public CompletableFuture<Void> updateEntityWithUsers(String namespace,
                                                         long id,
                                                         String entityKind,
                                                         List<Long> newUsers,
                                                         Entity updateEntity)
    {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        return datastore
            .exec(QueryBuilder.query(entityKind, id), txn)
            .thenApply(r -> {
                final Entity entity = Common.entityOrElseThrow(r);
                return buildUpdateMutations(namespace, newUsers, txn, entity, updateEntity);
            })
            .thenCompose(m -> datastore.execBatch(namespace, txn, m))
            .thenAccept(ignored -> {});
    }

    @Override
    public List<CompletableFuture<MutationStatement>> buildUpdateMutations(
        String namespace,
        List<Long> newUsers,
        CompletableFuture<TransactionResult> txn,
        Entity currentEntity,
        Entity newEntity)
    {
        final List<Long> currentUsers = Common.getIds(currentEntity, Constants.USERS);
        final List<CompletableFuture<MutationStatement>> mutations =
            getUserRefCountMutations(namespace, txn, currentUsers, newUsers);

        final long refCount = currentEntity.getInteger(Constants.REF_COUNT);
        final Entity updateEntity = Entity
            .builder(Entity.builder(newEntity).property(Constants.REF_COUNT, refCount).build())
            .key(currentEntity.getKey())
            .build();

        mutations.add(CompletableFuture.completedFuture(QueryBuilder.update(updateEntity)));

        return mutations;
    }

    @Override
    public List<CompletableFuture<MutationStatement>> buildInsertMutations(
        String namespace,
        Entity entity,
        List<Long> newUsers,
        CompletableFuture<TransactionResult> txn)
    {
        final List<CompletableFuture<MutationStatement>> mutations =
            getUserRefCountMutations(namespace, txn, ImmutableList.of(), newUsers);

        mutations.add(CompletableFuture.completedFuture(QueryBuilder.insert(entity)));

        return mutations;
    }

    private List<CompletableFuture<MutationStatement>> getUserRefCountMutations(
        String namespace,
        CompletableFuture<TransactionResult> txn,
        final List<Long> previousUsers,
        final List<Long> currentUsers) {
        final Set<Long> previous = Sets.newHashSet(previousUsers.iterator());
        final Set<Long> current = Sets.newHashSet(currentUsers.iterator());

        final Sets.SetView<Long> toAdd = Sets.difference(current, previous);
        final Sets.SetView<Long> toRemove = Sets.difference(previous, current);

        // Datastore only supports transactions over 25 different entity groups max.
        if (toAdd.size() + toRemove.size() > Common.MAX_CONCURRENT_TRANSACTIONS) {
            throw new ReferentialIntegrityException("too many users to add/remove");
        }

        // Prepare mutations
        final List<CompletableFuture<MutationStatement>> mutations = new ArrayList<>();

        // Add mutations to increase/decrease ref counter for users
        toAdd.stream().forEach(
            u -> mutations.add(userManager.increaseRefCounter(namespace, u, txn)));
        toRemove.stream().forEach(
            u -> mutations.add(userManager.decreaseRefCounter(namespace, u, txn)));

        return mutations;
    }
}
