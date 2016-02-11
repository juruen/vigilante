package io.vigilante.state.evaluator.core.impl.datastore;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.spotify.asyncdatastoreclient.*;
import com.spotify.futures.FuturesExtra;
import io.vigilante.state.evaluator.core.api.StateEvaluator;
import io.vigilante.state.evaluator.core.impl.datastore.model.EvaluationResult;
import io.viglante.common.model.IncidentState;
import io.viglante.common.model.InputStateEvaluator;
import io.viglante.common.model.OutputStateEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.ExecutorService;


@Slf4j
public class StateEvaluatorDatastore implements StateEvaluator {

    private final Random random = new Random();

    private final Datastore datastore;

    private final ExecutorService executorService;

    public StateEvaluatorDatastore(Datastore datastore, ExecutorService executorService) {
        this.datastore = datastore;
        this.executorService = executorService;
    }

    @Override
    public ListenableFuture<OutputStateEvaluator> evaluate(final InputStateEvaluator input) {
        final ListenableFuture<TransactionResult> txn = datastore.transactionAsync();
        final KeyQuery query = QueryBuilder.query(EvaluatorConsts.KIND, IncidentHelper.buildIncidentKey(input));

        ListenableFuture<QueryResult> res = FuturesExtra.asyncTransform(txn, r -> datastore.executeAsync(query, txn));

        return queryAndEvaluate(input, txn, res);
    }

    private ListenableFuture<OutputStateEvaluator> queryAndEvaluate(InputStateEvaluator input,
                                                                    final ListenableFuture<TransactionResult> txn,
                                                                    ListenableFuture<QueryResult> queryResultFuture)
    {
        ListenableFuture<OutputStateEvaluator> eval = FuturesExtra.asyncTransform(queryResultFuture,
            r ->
            {
                Entity entity = r.getEntity();

                if (entity == null) {
                    return insertIncident(input, txn);
                } else {
                    return updateIncident(input, txn, entity);
                }

            });


        final SettableFuture<OutputStateEvaluator> response = SettableFuture.create();

        Futures.addCallback(eval, new FutureCallback<OutputStateEvaluator>() {
            @Override
            public void onSuccess(OutputStateEvaluator result) {
                response.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("aborting transaction");

                datastore.rollbackAsync(txn);

                response.setException(t);
            }
        }, executorService);

        return response;
    }

    private ListenableFuture<OutputStateEvaluator> updateIncident(InputStateEvaluator input,
                                                                  ListenableFuture<TransactionResult> txn,
                                                                  Entity entity)
    {
        final EvaluationResult evaluationResult;
        try {
            evaluationResult = IncidentHelper.update(input, entity);
        } catch (InternalInconsistencyException e) {
            return Futures.immediateFailedFuture(e);
        }

        return FuturesExtra.syncTransform(buildUpdateOrDelete(input, txn, entity, evaluationResult),
            ignored -> inputToOutput(input, evaluationResult, entity));
    }

    private ListenableFuture<MutationResult> buildUpdateOrDelete(InputStateEvaluator input,
                                                                 ListenableFuture<TransactionResult> txn,
                                                                 Entity entity,
                                                                 EvaluationResult evaluationResult)
    {
        final ListenableFuture<MutationResult> mutation;
        if (evaluationResult.getState() == IncidentState.RESOLVED) {
            // FIXME Move incident to resolved incidents
            mutation = datastore.executeAsync(IncidentHelper.deleteQuery(input), txn);
        } else {
            mutation = datastore.executeAsync(IncidentHelper.updateQuery(entity, evaluationResult), txn);
        }
        return mutation;
    }

    private ListenableFuture<OutputStateEvaluator> insertIncident(InputStateEvaluator input,
                                                                  ListenableFuture<TransactionResult> txn)
    {
        if (input.getState() != IncidentState.TRIGGERED) {
            return Futures.immediateFuture(OutputStateEvaluator.builder().notify(false).build());
        }

        final long creationToken =  creationToken(input.getTimestamp());
        final String id = IncidentHelper.generateId();
        final Insert insert = IncidentHelper.insert(input, id, creationToken);

        return FuturesExtra.syncTransform(datastore.executeAsync(insert, txn),
            r -> inputToOutput(input, id, creationToken));
    }

    private OutputStateEvaluator inputToOutput(InputStateEvaluator input, String id, long creationToken) {
        return OutputStateEvaluator.builder()
            .company(input.getCompany())
            .creationToken(creationToken)
            .description(input.getDescription())
            .incident(input.getIncidentKey())
            .incidentId(id)
            .sequenceNumber(0L)
            .service(input.getServiceKey())
            .serviceId(input.getServiceId())
            .state(input.getState())
            .build();
    }

    private OutputStateEvaluator inputToOutput(InputStateEvaluator input,
                                               EvaluationResult evaluationResult,
                                               Entity entity)
    {
        return OutputStateEvaluator.builder()
            .company(input.getCompany())
            .creationToken(entity.getInteger(EvaluatorConsts.CREATION_TOKEN))
                .description(input.getDescription())
                .incident(input.getIncidentKey())
                .incidentId(entity.getString(EvaluatorConsts.INCIDENT_ID))
                .sequenceNumber(entity.getInteger(EvaluatorConsts.SEQUENCE) + 1)
                .service(input.getServiceKey())
                .serviceId(input.getServiceId())
                .state(evaluationResult.getState())
                .notify(evaluationResult.isNotify())
                .build();
    }


    private long creationToken(Long start) {
        return (start - (start % 1000)) + random.nextInt(1000);
    }
}
