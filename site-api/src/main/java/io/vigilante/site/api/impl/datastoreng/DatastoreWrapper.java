package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import com.spotify.asyncdatastoreclient.Update;
import com.spotify.futures.CompletableFuturesExtra;

import java.util.List;
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

    public CompletableFuture<QueryResult> exec(List<KeyQuery> queries,
                                               CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(queries, CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public CompletionStage<MutationResult> exec(Batch batch,
                                                CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(batch, CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public CompletableFuture<MutationResult> exec(Update update,
                                                  CompletableFuture<TransactionResult> txn) {
        return CompletableFuturesExtra.toCompletableFuture(
            datastore.executeAsync(update, CompletableFuturesExtra.toListenableFuture(txn)));
    }

    public  CompletableFuture<MutationResult> execBatch(
        String namespace,
        CompletableFuture<TransactionResult> txn,
        List<CompletableFuture<MutationStatement>> mutations) {
        return CompletableFuture
            .allOf(mutations.toArray(new CompletableFuture[mutations.size()]))
            .thenCompose(ignored -> {
                final Batch batch = QueryBuilder.batch();

                mutations
                    .stream()
                    .map(CompletableFuture::join)
                    .forEach(batch::add);

                return exec(batch, txn);
            });
    }
}
