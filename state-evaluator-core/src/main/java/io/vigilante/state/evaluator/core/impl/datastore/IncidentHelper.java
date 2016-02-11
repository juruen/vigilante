package io.vigilante.state.evaluator.core.impl.datastore;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.*;
import io.vigilante.state.evaluator.core.impl.datastore.model.EvaluationResult;
import io.vigilante.state.evaluator.core.impl.datastore.model.Event;
import io.viglante.common.model.IncidentState;
import io.viglante.common.model.InputStateEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class IncidentHelper {

    private static Entity  buildBasicEntity(final InputStateEvaluator input,
                                            final String incidentId,
                                            final String state,
                                            final long creationToken,
                                            final List<Object> states,
                                            final List<Object> timestamps)
    {
        return Entity.builder()
            .key(Key.builder(EvaluatorConsts.KIND, buildIncidentKey(input)).build())
            .property(EvaluatorConsts.COMPANY, input.getCompany())
            .property(EvaluatorConsts.SERVICE, input.getServiceKey())
            .property(EvaluatorConsts.SERVICE_ID, input.getServiceId())
            .property(EvaluatorConsts.INCIDENT_ID, incidentId)
            .property(EvaluatorConsts.TEAM_ID, input.getTeamId())
            .property(EvaluatorConsts.INCIDENT_KEY, input.getIncidentKey())
            .property(EvaluatorConsts.STATE, state)
            .property(EvaluatorConsts.LAST_UPDATE, input.getTimestamp())
            .property(EvaluatorConsts.CREATION_TIME, input.getTimestamp())
            .property(EvaluatorConsts.CREATION_TOKEN, creationToken)
            .property(EvaluatorConsts.STATES, states)
            .property(EvaluatorConsts.DESCRIPTION, input.getDescription())
            .property(EvaluatorConsts.TIMESTAMPS, timestamps)
            .property(EvaluatorConsts.SEQUENCE, 0)
            .build();
    }

    public static Insert insert(final InputStateEvaluator input, final String id, long creationToken) {
        List<Object> states = ImmutableList.of(input.getState().toString());
        List<Object> timestamps = ImmutableList.of(input.getTimestamp());

        Entity e = buildBasicEntity(input, id, IncidentState.TRIGGERED.toString(),
            creationToken, states, timestamps);

        return QueryBuilder.insert(e);
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static EvaluationResult update(final InputStateEvaluator input,
                                              final Entity entity) throws InternalInconsistencyException
    {
        List<IncidentState> states = getStates(entity);
        states.add(input.getState());

        List<Long> timestamps = getTimestamps(entity);
        timestamps.add(input.getTimestamp());

        checkConsistency(states, timestamps);

        IncidentState previousState = states.get(states.size() - 2);

        final List<Event> events = buildSortedEvents(states, timestamps);
        IncidentState currentState = events.get(events.size() - 1).getState();
        long currentTimestamp = events.get(events.size() - 1).getTimestamp();

        return EvaluationResult.builder()
            .state(currentState)
            .timestamp(currentTimestamp)
            .notify(notify(previousState, currentState))
            .timestamps(timestamps)
            .states(states)
            .build();
    }

    private static void checkConsistency(List<IncidentState> states,
                                         List<Long> timestamps) throws InternalInconsistencyException {
        if (states.size() != timestamps.size() || states.size() == 1) {
            throw new InternalInconsistencyException();
        }
    }

    public static String buildIncidentKey(InputStateEvaluator input) {
        return String.format("%s/%s/%s", input.getCompany(), input.getServiceKey(), input.getIncidentKey());
    }

    public static Update updateQuery(Entity entity, EvaluationResult evaluationResult) {
        return QueryBuilder.update(Entity.builder(entity)
            .property(EvaluatorConsts.STATES, evaluationResult.getStates())
            .property(EvaluatorConsts.TIMESTAMPS, evaluationResult.getTimestamps())
            .property(EvaluatorConsts.TIMESTAMPS, evaluationResult.getTimestamp())
            .property(EvaluatorConsts.STATE, evaluationResult.getState())
            .property(EvaluatorConsts.SEQUENCE, entity.getInteger(EvaluatorConsts.SEQUENCE) + 1)
            .build());
    }

    private static boolean notify(IncidentState previousState, IncidentState currentState) {
        if (previousState == currentState) {
            return false;
        }

        switch (previousState) {
            case RESOLVED:
                switch (currentState) {
                    case TRIGGERED:
                        return true;
                    default:
                        return false;
                }
            case TRIGGERED:
                switch (currentState) {
                    case TRIGGERED:
                        return false;
                    default:
                        return false;
                }
            case ACKNOWLEDGED:
                switch (currentState) {
                    case RESOLVED:
                        return true;
                    default:
                        return false;
                }
        }

        return false;
    }

    private static List<Event> buildSortedEvents(List<IncidentState> states, List<Long> timestamps) {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < states.size(); i++) {
            events.add(Event.builder()
                .state(states.get(i))
                .timestamp(timestamps.get(i))
                .build());
        }

        return events.stream()
            .sorted((e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()))
            .collect(Collectors.toList());
    }

    private static List<Long> getTimestamps(Entity e) {
        return e.getList(EvaluatorConsts.TIMESTAMPS)
            .stream()
            .map((Value s) -> s.getInteger())
            .collect(Collectors.toList());
    }

    private static List<IncidentState> getStates(Entity e) {
        return e.getList(EvaluatorConsts.STATES)
            .stream()
            .map(((Value s) -> IncidentState.valueOf(s.getString())))
            .collect(Collectors.toList());
    }


    public static Delete deleteQuery(InputStateEvaluator key) {
        return QueryBuilder.delete(EvaluatorConsts.KIND, buildIncidentKey(key));
    }
}
