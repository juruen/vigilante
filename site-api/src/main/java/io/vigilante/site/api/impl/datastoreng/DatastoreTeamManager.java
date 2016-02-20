package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.Sets;
import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import com.spotify.asyncdatastoreclient.Value;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.AddTeam;
import io.viglante.common.model.Team;
import io.viglante.common.model.UpdateTeam;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DatastoreTeamManager {

    private static final int MAX_CONCURRENT_TRANSACTIONS = 24;
    private final DatastoreWrapper datastore;
    private final UserOps userOps;

    public DatastoreTeamManager(final DatastoreWrapper datastore, final UserOps userOps) {
        this.datastore = datastore;
        this.userOps = userOps;
    }

    public CompletableFuture<List<Team>> getTeams(final @NonNull String namespace) {
        return datastore
            .exec(QueryBuilder.query().kindOf(Constants.TEAM_KIND))
            .thenApply(this::buildTeamList);
    }

    public CompletableFuture<Long> addTeam(final @NonNull String namespace,
                                           final @NonNull AddTeam team) {
        return datastore
            .exec(QueryBuilder.insert(buildTeamEntity(team, 0)))
            .thenApply(r -> r.getInsertKey().getId());
    }

    public CompletableFuture<Void> updateTeam(final @NonNull String namespace,
                                              final long id,
                                              final @NonNull UpdateTeam team) {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        return datastore
            .exec(QueryBuilder.query(Constants.TEAM_KIND, id), txn)
            .thenCompose(r -> update(namespace, txn, team, r))
            .thenAccept(ignored -> {
            });
    }

    private CompletableFuture<MutationResult> update(
        String namespace,
        CompletableFuture<TransactionResult> txn,
        UpdateTeam team,
        QueryResult queryResult) {
        final List<CompletableFuture<MutationStatement>> mutations =
            getUserRefCountMutations(namespace, txn, team, queryResult);

        // Add mutation to update team
        final Entity updatedTeam =
            buildUpdateTeamEntity(team, queryResult.getEntity().getInteger(Constants.REF_COUNT));

        mutations.add(CompletableFuture.completedFuture(QueryBuilder.update(updatedTeam)));

        // Create a CompletableFuture with a list of all mutations
        return CompletableFuture
            .allOf(mutations.toArray(new CompletableFuture[mutations.size()]))
            .thenCompose(ignored -> {
                final Batch batch = QueryBuilder.batch();

                mutations
                    .stream()
                    .map(CompletableFuture::join)
                    .forEach(batch::add);

                return datastore.exec(batch, txn);
            });
    }

    private List<CompletableFuture<MutationStatement>> getUserRefCountMutations(
        String namespace,
        CompletableFuture<TransactionResult> txn,
        UpdateTeam team,
        QueryResult queryResult) {
        // Figure out users to add and users to remove
        final Set<Long> newUsers = team.getUsers().stream().collect(Collectors.toSet());
        final Set<Long> currentUsers = Common.getList(queryResult.getEntity(), Constants.USERS)
            .stream().map(Value::getInteger)
            .collect(Collectors.toSet());

        final Sets.SetView<Long> toAdd = Sets.difference(newUsers, currentUsers);
        final Sets.SetView<Long> toRemove = Sets.difference(currentUsers, newUsers);

        // Datastore only supports transactions over 25 different entity groups max.
        if (toAdd.size() + toRemove.size() > MAX_CONCURRENT_TRANSACTIONS) {
            throw new ReferentialIntegrityException("too many users to add/remove");
        }

        // Prepare mutations
        final List<CompletableFuture<MutationStatement>> mutations = new ArrayList<>();

        // Add mutations to increase/decrease ref counter for users
        toAdd.stream().forEach(u -> mutations.add(userOps.increaseRefCounter(namespace, u, txn)));
        toRemove.stream().forEach(u -> mutations.add(userOps.decreaseRefCounter(namespace, u, txn)));

        return mutations;
    }

    public CompletableFuture<Team> getTeam(final @NonNull String namespace, final long id) {
        return datastore
            .exec(QueryBuilder.query(Constants.TEAM_KIND, id))
            .thenApply(r -> buildTeam(r.getEntity()));
    }

    public CompletableFuture<Void> deleteTeam(final @NonNull String namespace,
                                              final long id) {
        final CompletableFuture<TransactionResult> txn = datastore.txn();

        final Key key = Key.builder(Constants.TEAM_KIND, id).build();

        return datastore
            .exec(QueryBuilder.query(key), txn)
            .thenAccept(
                r -> {
                    if (r.getEntity().getInteger(Constants.REF_COUNT) != 0) {
                        throw new ReferentialIntegrityException("team is being used");
                    }

                    if (!Common.getList(r.getEntity(), Constants.USER).isEmpty()) {
                        throw new ReferentialIntegrityException("team contains users");
                    }
                })
            .thenAcceptBoth(datastore.exec(QueryBuilder.delete(key), txn), (a, b) -> {
            });
    }

    public CompletableFuture<Batch> increaseRefCounter(
        final @NonNull String namespace,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn) {
        return Common.increaseRefCounter(namespace, datastore, Constants.TEAM_KIND, id, txn, 1);
    }

    public CompletableFuture<Batch> decreaseRefCounter(
        final @NonNull String namespace,
        final long id,
        final @NonNull CompletableFuture<TransactionResult> txn) {
        return Common.increaseRefCounter(namespace, datastore, Constants.TEAM_KIND, id, txn, -1);
    }

    private CompletableFuture<Void> updateTeamSameRefCount(UpdateTeam team,
                                                           CompletableFuture<TransactionResult> txn,
                                                           QueryResult r) {
        final long refCount = r.getEntity().getInteger(Constants.REF_COUNT);

        return datastore
            .exec(QueryBuilder.insert(buildUpdateTeamEntity(team, refCount)), txn)
            .thenAccept(ignored -> {
            });
    }

    private Entity buildTeamEntity(@NonNull AddTeam team, long refCount) {
        final Entity.Builder entity = Entity.builder(Constants.TEAM_KIND)
            .property(Constants.NAME, team.getName())
            .property(Constants.REF_COUNT, refCount);

        return entity.build();
    }

    private Entity buildUpdateTeamEntity(@NonNull UpdateTeam team, long refCount) {
        final Entity.Builder entity = Entity.builder(Constants.TEAM_KIND)
            .property(Constants.NAME, team.getName())
            .property(Constants.USERS, team.getUsers())
            .property(Constants.REF_COUNT, refCount);

        return entity.build();
    }

    private List<Team> buildTeamList(QueryResult result) {
        return result.getAll().stream()
            .map(this::buildTeam)
            .collect(Collectors.toList());
    }

    private Team buildTeam(Entity entity) {
        final List<Long> users = Common.getList(entity, Constants.USERS)
            .stream()
            .map(Value::getInteger)
            .collect(Collectors.toList());

        return Team.builder()
            .id(Optional.of(entity.getKey().getId()))
            .name(entity.getString(Constants.NAME))
            .users(users)
            .build();
    }
}
