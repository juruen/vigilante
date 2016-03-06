package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import io.vigilante.site.impl.datastore.basic.Constants;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RelationManager {
    private final Relations relations;
    private final DatastoreWrapper datastore;

    public RelationManager(@NonNull Relations relations, @NonNull DatastoreWrapper datastore) {
        this.relations = relations;
        this.datastore = datastore;
    }

    public CompletableFuture<String> addEntity(
        @NonNull Entity entity,
        @NonNull EntityDescriptor descriptor,
        @NonNull List<TargetReferences> targets
    ) {
        return update(
            datastore.txn(),
            SourceReference.Type.ADD,
            entity,
            descriptor,
            targets,
            ImmutableList.of()
        );
    }

    public CompletableFuture<Void> updateEntity(
        @NonNull CompletableFuture<TransactionResult> txn,
        @NonNull Entity entity,
        @NonNull EntityDescriptor descriptor,
        @NonNull List<TargetReferences> addTargets,
        @NonNull List<TargetReferences> delTargets
    ) {
        return update(
            txn,
            SourceReference.Type.UPDATE,
            entity,
            descriptor,
            addTargets,
            delTargets
        ).thenApply(r -> null);
    }

    public CompletableFuture<Void> updateReverseEntity(
        @NonNull CompletableFuture<TransactionResult> txn,
        @NonNull Entity entity,
        @NonNull EntityDescriptor descriptor
    ) {
        final SourceReference source = SourceReference.builder()
            .descriptor(descriptor)
            .id(entity.getKey().getName())
            .name(entity.getString(Constants.NAME))
            .build();

        final List<KeyQuery> targets = relations
            .getReverseRelationsFor(descriptor)
            .stream()
            .flatMap(r ->
                Common
                    .getStringList(entity, r.ids())
                    .stream()
                    .map(i -> QueryBuilder.query(r.kind(), i))
            )
            .collect(Collectors.toList());

        return datastore
            .exec(targets, txn)
            .thenCompose(result -> {
                final Batch batch = QueryBuilder.batch();

                result
                    .getAll()
                    .stream()
                    .forEach(r -> batch.add(QueryBuilder.update(updateReference(source, r, true))));

                batch.add(QueryBuilder.update(entity));

                return datastore.exec(batch, txn);
            })
            .thenAccept(ignored -> {});
    }

    private CompletableFuture<String> update(
        CompletableFuture<TransactionResult> txn,
        SourceReference.Type type,
        Entity entity,
        EntityDescriptor descriptor,
        List<TargetReferences> addTargets,
        List<TargetReferences> delTargets)
    {
        final Key key = getOrGenerateKey(type, entity, descriptor);

        final List<KeyQuery> targetQueries = buildTargetQueries(addTargets, delTargets);
        final String name = entity.getString(Constants.NAME);

        return datastore
            .exec(targetQueries, txn)
            .thenCompose(r ->
            {
                final SourceReference source = SourceReference
                    .builder()
                    .type(type)
                    .descriptor(descriptor)
                    .id(key.getName())
                    .name(name)
                    .entity(Entity.builder(entity).key(key).build())
                    .build();

                final Batch batch = buildMutationBatch(source, addTargets, delTargets, r);

                return datastore.exec(batch, txn)
                    .thenApply(ignored -> key.getName());
            });
    }

    private List<KeyQuery> buildTargetQueries(List<TargetReferences> addTargets,
                                              List<TargetReferences> delTargets)
    {
        final List<KeyQuery> targetQueries = prepareTargetQueries(addTargets);
        targetQueries.addAll(prepareTargetQueries(delTargets));
        return targetQueries;
    }

    private Key getOrGenerateKey(SourceReference.Type type,
                                 Entity entity,
                                 EntityDescriptor descriptor)
    {
        if (type == SourceReference.Type.ADD) {
            return Key.builder(descriptor.kind(), id()).build();
        } else {
            return entity.getKey();
        }
    }

    private Batch buildMutationBatch(SourceReference source,
                                     List<TargetReferences> addTargets,
                                     List<TargetReferences> delTargets,
                                     QueryResult result)
    {
        final Set<String> idsToAdd = addTargets
            .stream()
            .flatMap(t -> t.getIds().stream())
            .collect(Collectors.toSet());

        final Set<String> idsToRemove = delTargets
            .stream()
            .flatMap(t -> t.getIds().stream())
            .collect(Collectors.toSet());

        final Batch batch = updateTargets(source, idsToAdd, idsToRemove, result);

        final Map<String, Map<String, String>> targetNames = getTargetNames(idsToAdd, result);
        final Entity sourceEntity = updateSource(source, delTargets, targetNames);

        if (source.getType() == SourceReference.Type.ADD) {
            batch.add(QueryBuilder.insert(sourceEntity));
        } else {
            batch.add(QueryBuilder.update(sourceEntity));
        }

        return batch;
    }

    private Entity updateSource(
        SourceReference source,
        List<TargetReferences> delTargets,
        Map<String, Map<String, String>> targetNames
    ) {
        final Entity step1 = updateSourceWithTargets(source, targetNames);
        return updateSourceWithRemovedTargets(step1, delTargets);
    }

    private Entity updateSourceWithTargets(
        SourceReference source,
        Map<String, Map<String, String>> targetNames
    ) {
        Entity sourceEntity = source.getEntity();

        for (final Map.Entry<String, Map<String, String>> kv : targetNames.entrySet()) {
            for (final Map.Entry<String, String> idNamePair : kv.getValue().entrySet()) {
                final String id = idNamePair.getKey();
                final String name = idNamePair.getValue();

                final SourceReference reverseSource = SourceReference
                    .builder()
                    .descriptor(EntityDescriptor.fromKind(kv.getKey()))
                    .id(id)
                    .name(name)
                    .build();

                    sourceEntity = updateReference(reverseSource, sourceEntity, true);
            }
        }

        return sourceEntity;
    }

    private Entity updateSourceWithRemovedTargets(
        Entity entity,
        List<TargetReferences> delTargets
    ) {
        Entity sourceEntity = entity;

        for (final TargetReferences target : delTargets) {
            for (final String id : target.getIds()) {
                final SourceReference reverseSource = SourceReference
                    .builder()
                    .descriptor(target.getDescriptor())
                    .id(id)
                    .build();

                sourceEntity = updateReference(reverseSource, sourceEntity, false);
            }
        }

        return sourceEntity;
    }

    private Batch updateTargets(
        SourceReference source,
        Set<String> idsToAdd,
        Set<String> idsToRemove,
        QueryResult result)
    {
        final Batch batch = QueryBuilder.batch();

        for (final Entity entity : result.getAll()) {
            if (idsToAdd.contains(entity.getKey().getName())) {
                batch.add(QueryBuilder.update(updateReference(source, entity, true)));
            } else if (idsToRemove.contains(entity.getKey().getName())) {
                batch.add(QueryBuilder.update(updateReference(source, entity, false)));
            }
        }

        return batch;
    }

    private Map<String, Map<String, String>> getTargetNames(
        Set<String> idsToAdd,
        QueryResult result)
    {
        final Map<String, Map<String, String>> targetNames = new HashMap<>();

        result
            .getAll()
            .stream()
            .filter(e -> idsToAdd.contains(e.getKey().getName()))
            .forEach(e -> {
                final String kind = e.getKey().getKind();
                final String id = e.getKey().getName();

                targetNames.putIfAbsent(kind, new HashMap<>());
                targetNames.get(kind).put(id, e.getString(Constants.NAME));
            });

        return targetNames;
    }

    private Entity updateReference(
        SourceReference source,
        Entity entity,
        boolean add
    ) {
        final Map<String, String> names = Common
            .getStringMap(entity, source.getDescriptor().names());

        final Set<String> ids = Common
            .getStringList(entity, source.getDescriptor().ids())
            .stream()
            .collect(Collectors.toSet());

        if (add) {
            ids.add(source.getId());
            names.put(source.getId(), source.getName());
        } else {
            ids.remove(source.getId());
            names.remove(source.getId());
        }

        final List<Object> idList = Common.toList(ids.stream().collect(Collectors.toList()));

        return Entity
            .builder(entity)
            .property(source.getDescriptor().ids(), idList)
            .property(source.getDescriptor().names(), Common.toEntity(names))
            .build();
    }

    private List<KeyQuery> prepareTargetQueries(List<TargetReferences> targets) {
        return targets
            .stream()
            .filter(t -> !t.getIds().isEmpty())
            .flatMap(t -> t
                .getIds()
                .stream()
                .map(r -> QueryBuilder.query(t.getDescriptor().kind(), r)))
            .collect(Collectors.toList());
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}
