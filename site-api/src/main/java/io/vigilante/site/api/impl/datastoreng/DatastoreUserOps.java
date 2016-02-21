package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.TransactionResult;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

public class DatastoreUserOps implements UserOps {
    private final DatastoreUserManager userManager;

    public DatastoreUserOps(DatastoreUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public CompletableFuture<MutationStatement> increaseRefCounter(
        String namespace,
        long id,
        @NonNull CompletableFuture<TransactionResult> txn) {
        return userManager.increaseRefCounter(namespace, id, txn);
    }

    @Override
    public CompletableFuture<MutationStatement> decreaseRefCounter(
        String namespace,
        long id,
        @NonNull CompletableFuture<TransactionResult> txn) {
        return userManager.decreaseRefCounter(namespace, id, txn);
    }
}
