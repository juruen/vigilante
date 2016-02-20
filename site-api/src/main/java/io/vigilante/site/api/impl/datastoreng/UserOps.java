package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.TransactionResult;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

public interface UserOps {
    CompletableFuture<MutationStatement> increaseRefCounter(
        String namespace,
        long id,
        final @NonNull CompletableFuture<TransactionResult> txn);

    CompletableFuture<MutationStatement> decreaseRefCounter(
        String namespace,
        long id,
        final @NonNull CompletableFuture<TransactionResult> txn);
}
