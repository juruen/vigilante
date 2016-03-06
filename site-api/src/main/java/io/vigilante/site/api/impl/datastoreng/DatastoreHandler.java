package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.TransactionResult;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.spotify.asyncdatastoreclient.QueryBuilder.asc;

public class DatastoreHandler {
    private final DatastoreWrapper datastore;
    private final RelationManager relations;

    public DatastoreHandler(DatastoreWrapper datastore, RelationManager relations) {
        this.datastore = datastore;
        this.relations = relations;
    }

    public <T> CompletableFuture<List<T>> fetchEntities(
        String ignored,
        EntityDescriptor descriptor,
        EntityTranslator<T> translator
    ) {
        return datastore
            .exec(
                QueryBuilder
                    .query()
                    .kindOf(descriptor.kind())
                    .orderBy(asc(Constants.NAME)))
            .thenApply(r ->
                r
                    .getAll()
                    .stream()
                    .map(translator::translate).
                    collect(Collectors.toList())
            );
    }


    public CompletableFuture<String> addEntity(
        String ignored,
        EntityDescriptor descriptor,
        Entity entity
    ) {
        return relations
            .addEntity(entity, descriptor, ImmutableList.of());
    }

    public CompletableFuture<Void> updateEntity(
        String ignored,
        EntityDescriptor descriptor,
        String id,
        Entity newEntity
    ) {

        final CompletableFuture<TransactionResult> txn = datastore.txn();
        final Key key = Key.builder(descriptor.kind(), id).build();
        final Entity entity = Entity.builder(newEntity).key(key).build();

        return datastore
            .exec(QueryBuilder.query(key), txn)
            .thenCompose(r -> {
                final Entity storedEntity = Common.entityOrElseThrow(r);

                final ReferenceDelta delta = buildReferenceDelta(descriptor, storedEntity, entity);

                if (Common.hasSameName(storedEntity, newEntity)) {
                    return relations.updateEntity(
                        txn, entity, descriptor, delta.getToAdd(), delta.getToRemove(), false);
                } else {
                    return relations.updateEntity(
                        txn, entity, descriptor, delta.getCurrent(), delta.getToRemove(), true);
                }
            });
    }

    public CompletableFuture<Void> deleteEntity(
        String ignored,
        EntityDescriptor descriptor,
        String id
    ) {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        return datastore
            .exec(QueryBuilder.query(descriptor.kind(), id), txn)
            .thenCompose(r -> {
                final Entity storedEntity = Common.entityOrElseThrow(r);

                if (entityInUse(descriptor, storedEntity)) {
                    throw new ReferentialIntegrityException("entity in use");
                }

                return datastore
                    .exec(QueryBuilder.delete(descriptor.kind(), id), txn)
                    .thenAccept(i -> {});
            });
    }

    private boolean entityInUse(EntityDescriptor descriptor, Entity storedEntity) {
        for (EntityDescriptor d : relations.relations().getReverseRelationsFor(descriptor)) {
            if (!Common.getStringList(storedEntity, d.ids()).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public <T> CompletableFuture<T> fetchEntity(
        String ignored,
        EntityDescriptor descriptor,
        String id,
        EntityTranslator<T> entityTransalator
    ) {
        return datastore
            .exec(QueryBuilder.query(descriptor.kind(), id))
            .thenApply(r -> {
                final Entity storedEntity = Common.entityOrElseThrow(r);
                return entityTransalator.translate(storedEntity);
            });
    }

    private ReferenceDelta buildReferenceDelta(
        EntityDescriptor descriptor,
        Entity oldEntity,
        Entity newEntity
    ) {
        final List<TargetReferences> toLeave = new ArrayList<>();
        final List<TargetReferences> toAdd = new ArrayList<>();
        final List<TargetReferences> toRemove = new ArrayList<>();
        final List<TargetReferences> current = new ArrayList<>();

        for (EntityDescriptor d : relations.relations().getRelationsFor(descriptor)) {
            final Set<String> oldIds =
                Common.getStringList(oldEntity, d.ids()).stream().collect(Collectors.toSet());
            final Set<String> newIds =
                Common.getStringList(newEntity, d.ids()).stream().collect(Collectors.toSet());

            // Ids to add
            List<String> idsToAdd = Sets
                .difference(newIds, oldIds)
                .stream()
                .collect(Collectors.toList());

            toAdd.add(new TargetReferences(d, idsToAdd));

            // Ids to remove
            List<String> idsToRemove = Sets
                .difference(oldIds, newIds)
                .stream()
                .collect(Collectors.toList());

            toRemove.add(new TargetReferences(d, idsToRemove));

            // Ids to leave
            List<String> idsToLeave = Sets
                .intersection(oldIds, newIds)
                .stream()
                .collect(Collectors.toList());

            toLeave.add(new TargetReferences(d, idsToLeave));

            current.add(new TargetReferences(d, newIds.stream().collect(Collectors.toList())));
        }

        return ReferenceDelta.builder()
            .current(current)
            .toAdd(toAdd)
            .toRemove(toRemove)
            .toLeave(toLeave)
            .build();
    }
}
