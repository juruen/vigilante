package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.List;
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
        String namespace,
        EntityDescriptor descriptor,
        String id,
        Entity entity
    ) {

        /*
        datastore
            .exec(QueryBuilder.query(descriptor.kind(), id))
            .thenCompose(r -> {
                final Entity storedEntity = Common.entityOrElseThrow(r);

                if (Common.hasSameName(storedEntity, entity)) {

                } else {

                }
            })
            */

        return null;
    }

    public CompletableFuture<Void> deleteEntity(
        String namespace,
        EntityDescriptor descriptor,
        String id
    ) {
        return null;
    }

    public <T> CompletableFuture<T> fetchEntity(
        String namespace,
        EntityDescriptor descriptor,
        String id,
        EntityTranslator<Object> entityToUser)
    {
        return null;
    }
}
