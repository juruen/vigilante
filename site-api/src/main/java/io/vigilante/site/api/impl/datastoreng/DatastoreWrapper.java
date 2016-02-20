package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import com.spotify.futures.CompletableFuturesExtra;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DatastoreWrapper {
    final private com.spotify.asyncdatastoreclient.Datastore datastore;

    public DatastoreWrapper(com.spotify.asyncdatastoreclient.Datastore datastore) {
        this.datastore = datastore;
    }

    public CompletableFuture<TransactionResult> txn() {
        return CompletableFuturesExtra.toCompletableFuture(datastore.transactionAsync());
    }

    public CompletableFuture<QueryResult> exec(Query query) {
        return CompletableFuturesExtra.toCompletableFuture(datastore.executeAsync(query));
    }

    public CompletableFuture<QueryResult> exec(KeyQuery query) {
        return CompletableFuturesExtra.toCompletableFuture(datastore.executeAsync(query));
    }

    public CompletableFuture<MutationResult> exec(Insert insert) {
        return CompletableFuturesExtra.toCompletableFuture(datastore.executeAsync(insert));
    }

    public CompletableFuture<MutationResult> exec(Insert insert,
                                                  CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(insert, CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public CompletableFuture<MutationResult> exec(Delete delete,
                                                  CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(datastore.executeAsync(delete,
            CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public CompletableFuture<QueryResult> exec(KeyQuery query,
                                               CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(query, CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public CompletionStage<MutationResult> exec(Batch batch,
                                                CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(batch, CompletableFuturesExtra.toListenableFuture(txn)));
    }
}
