package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.TransactionResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UserIntegrity {
    CompletableFuture<Long> inertEntityWithUsers(
        String namespace,
        Entity addEntity,
        List<Long> newUsers);

    CompletableFuture<Void> updateEntityWithUsers(
        String namespace,
        long id,
        String entityKind,
        List<Long> newUsers,
        Entity updateEntity);

    List<CompletableFuture<MutationStatement>> buildUpdateMutations(
        String namespace,
        List<Long> newUsers,
        CompletableFuture<TransactionResult> txn,
        Entity currentEntity,
        Entity newEntity);

    List<CompletableFuture<MutationStatement>> buildInsertMutations(
        String namespace,
        Entity entity,
        List<Long> newUsers,
        CompletableFuture<TransactionResult> txn);
}
