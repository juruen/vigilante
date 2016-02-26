package io.vigilante.site.api.impl.datastoreng;

import com.spotify.asyncdatastoreclient.Batch;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;
import com.spotify.asyncdatastoreclient.TransactionResult;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.Team;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.spotify.asyncdatastoreclient.QueryBuilder.asc;

public class DatastoreTeamManager {

    private final DatastoreWrapper datastore;
    private final UserIntegrity userOps;

    public DatastoreTeamManager(final DatastoreWrapper datastore, final UserIntegrity userOps) {
        this.datastore = datastore;
        this.userOps = userOps;
    }

    public CompletableFuture<List<Team>> getTeams(final @NonNull String namespace) {
        return datastore
            .exec(QueryBuilder
                .query()
                .kindOf(Constants.TEAM_KIND)
                .orderBy(asc(Constants.NAME)))
            .thenApply(this::buildTeamList);
    }

    public CompletableFuture<Long> addTeam(final @NonNull String namespace,
                                           final @NonNull Team team)
    {
        final Entity addEntity = buildTeamEntity(team, Optional.of(0L));
        final List<Long> newUsers = team.getUsers();

        return userOps.inertEntityWithUsers(namespace, addEntity, newUsers);
    }

    public CompletableFuture<Void> updateTeam(final @NonNull String namespace,
                                              final long id,
                                              final @NonNull Team team) {

        final String entityKind = Constants.TEAM_KIND;
        final List<Long> newUsers = team.getUsers();
        final Entity updateEntity = buildTeamEntity(team, Optional.empty());

        return userOps.updateEntityWithUsers(namespace, id, entityKind, newUsers, updateEntity);
    }


    public CompletableFuture<Team> getTeam(final @NonNull String namespace, final long id) {
        return datastore
            .exec(QueryBuilder.query(Constants.TEAM_KIND, id))
            .thenApply(Common::entityOrElseThrow)
            .thenApply(this::buildTeam);
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

                    if (!Common.getList(r.getEntity(), Constants.USERS).isEmpty()) {
                        throw new ReferentialIntegrityException("team contains users");
                    }
                })
            .thenCompose(ignored -> datastore.exec(QueryBuilder.delete(key), txn))
            .thenAccept(ignored -> {
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

    private CompletableFuture<Void> teamSameRefCount(Team team,
                                                     CompletableFuture<TransactionResult> txn,
                                                     QueryResult r) {
        final long refCount = r.getEntity().getInteger(Constants.REF_COUNT);

        return datastore
            .exec(QueryBuilder.insert(buildTeamEntity(team, Optional.of(refCount))), txn)
            .thenAccept(ignored -> {
            });
    }

    private Entity buildTeamEntity(Team team, Optional<Long> refCount) {
        final Entity.Builder entity = Entity.builder(Constants.TEAM_KIND)
            .property(Constants.NAME, team.getName())
            .property(Constants.USERS, Common.toList(team.getUsers()));

        refCount.ifPresent(c -> entity.property(Constants.REF_COUNT, c));

        return entity.build();
    }

    private List<Team> buildTeamList(QueryResult result) {
        return result.getAll().stream()
            .map(this::buildTeam)
            .collect(Collectors.toList());
    }

    private Team buildTeam(Entity entity) {
        return Team.builder()
            .id(Optional.of(entity.getKey().getId()))
            .name(entity.getString(Constants.NAME))
            .users(Common.getIds(entity, Constants.USERS))
            .build();
    }
}
