package io.vigilante.site.api.impl.datastoreng;

import com.spotify.asyncdatastoreclient.Entity;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.Team;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DatastoreTeamManager {
    private final static EntityDescriptor DESCRIPTOR = EntityDescriptor.TEAM;

    private final DatastoreHandler datastore;

    public DatastoreTeamManager(DatastoreHandler datastore) {
        this.datastore = datastore;
    }

    public CompletableFuture<List<Team>> getTeams(final @NonNull String namespace) {
        return datastore.fetchEntities(namespace, DESCRIPTOR, this::entityToTeam);
    }

    public CompletableFuture<String> addTeam(
        final @NonNull String namespace, final @NonNull Team team
    ) {
        return datastore.addEntity(namespace, DESCRIPTOR, teamToEntity(team));
    }

    public CompletableFuture<Void> updateTeam(
        final @NonNull String namespace, final String id, final @NonNull Team team)
    {
        return datastore.updateEntity(namespace, DESCRIPTOR, id, teamToEntity(team));
    }

    public CompletableFuture<Void> deleteTeam(
        final @NonNull String namespace, final String id)
    {
        return datastore.deleteEntity(namespace, DESCRIPTOR, id);
    }

    public CompletableFuture<Team> getTeam(
        final @NonNull String namespace,
        final String id)
    {
        return datastore.fetchEntity(namespace, DESCRIPTOR, id, this::entityToTeam);
    }

    private Entity teamToEntity(Team team) {
        final Entity.Builder entity = Entity.builder()
            .property(Constants.NAME, team.getName())
            .property(EntityDescriptor.USER.ids(), Common.toList(team.getUsers()));

        return entity.build();
    }

    private Team entityToTeam(Entity entity) {
        return Team.builder()
            .id(Optional.of(entity.getKey().getName()))
            .name(entity.getString(Constants.NAME))
            .users(Common.getStringList(entity, EntityDescriptor.USER.ids()))
            .build();
    }
}
