package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreConfig;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import io.vigilante.site.impl.datastore.basic.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertEquals;


public class RelationManagerTest {

    @Rule
    public Timeout globalTimeout = new Timeout(3000000);

    public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8081");
    public static final String DATASET_ID = System.getProperty("dataset", "test");
    public static final String NAMESPACE = System.getProperty("namespace", "test");

    public static final int TIMEOUT = 5;

    protected static Datastore datastore;
    protected static DatastoreWrapper datastoreWrapper;

    protected RelationManager relationManger;

    @Before
    public void setUp() throws Exception {
        datastore = Datastore.create(datastoreConfig());
        resetDatastore();

        datastoreWrapper = new DatastoreWrapper(datastore);

        final Entity user1 = Entity
            .builder(EntityDescriptor.USER.kind(), "user1")
            .property("name", "user1")
            .build();
        datastore.execute(QueryBuilder.insert(user1));

        final Entity user2 = Entity
            .builder(EntityDescriptor.USER.kind(), "user2")
            .property("name", "user2")
            .build();
        datastore.execute(QueryBuilder.insert(user2));

        final Entity escalaion1 = Entity
            .builder(EntityDescriptor.ESCALATION.kind(), "escalation1")
            .property("name", "escalation1")
            .build();
        datastore.execute(QueryBuilder.insert(escalaion1));

        final Relations relations = Relations.builder()
            .addRelation(EntityDescriptor.TEAM, EntityDescriptor.USER)
            .addRelation(EntityDescriptor.TEAM, EntityDescriptor.ESCALATION)
            .addRelation(EntityDescriptor.SERVICE, EntityDescriptor.TEAM)
            .build();

        relationManger = new RelationManager(relations, datastoreWrapper);
    }

    @Test
    public void testAddTeamWithUserAndEscalation() throws Exception {
        final Entity team1 = Entity
            .builder(EntityDescriptor.TEAM.kind(), "team1")
            .property("name", "team1")
            .property(EntityDescriptor.USER.ids(), Common.toList(ImmutableList.of("user1")))
            .build();

        final String teamId = relationManger.addEntity(
            team1,
            EntityDescriptor.TEAM,
            ImmutableList.of(
                new TargetReferences(EntityDescriptor.USER, "user1"),
                new TargetReferences(EntityDescriptor.ESCALATION, "escalation1")))
            .get();

        final Entity storedTeamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity storedUserEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();

        final Entity storedEscalationEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.ESCALATION.kind(), "escalation1"))
            .getEntity();

        assertEquals(
            ImmutableMap.of("user1", "user1"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.USER.names()));

        assertEquals(
            ImmutableList.of("user1"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.USER.ids()));

        assertEquals(
            ImmutableMap.of("escalation1", "escalation1"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.ESCALATION.names()));

        assertEquals(
            ImmutableList.of("escalation1"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.ESCALATION.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedUserEntity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUserEntity, EntityDescriptor.TEAM.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedEscalationEntity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedEscalationEntity, EntityDescriptor.TEAM.ids()));
    }

    @Test
    public void testUpdateTeamWithUser() throws Exception {
        final Entity team1 = Entity
            .builder(EntityDescriptor.TEAM.kind(), "team1")
            .property("name", "team1")
            .property(EntityDescriptor.USER.ids(), Common.toList(ImmutableList.of("user1")))
            .build();

        final String teamId = relationManger.addEntity(
            team1,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user1"))).get();

        final Entity teamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity teamEntityTwoUsers = Entity
            .builder(teamEntity)
            .property(EntityDescriptor.TEAM.ids(), ImmutableList.of("user1", "user2"))
            .build();

        relationManger.updateEntity(
            datastoreWrapper.txn(),
            teamEntityTwoUsers,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user2")),
            ImmutableList.of(),
            false).get();

        final Entity storedTeamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity storedUser1Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();

        final Entity storedUser2Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user2"))
            .getEntity();


        assertEquals(
            ImmutableMap.of("user1", "user1", "user2", "user2"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.USER.names()));

        assertEquals(
            ImmutableList.of("user1", "user2"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.USER.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedUser1Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUser1Entity, EntityDescriptor.TEAM.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedUser2Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUser2Entity, EntityDescriptor.TEAM.ids()));

    }

    @Test
    public void testUpdateTeamWithUserAndService() throws Exception {
        final Entity team1 = Entity
            .builder(EntityDescriptor.TEAM.kind(), "team1")
            .property("name", "team1")
            .property(EntityDescriptor.USER.ids(), Common.toList(ImmutableList.of("user1")))
            .build();

        final String teamId = relationManger.addEntity(
            team1,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user1"))).get();

        final Entity service1 = Entity
            .builder(EntityDescriptor.SERVICE.kind(), "service1")
            .property("name", "service1")
            .property(EntityDescriptor.TEAM.ids(), Common.toList(ImmutableList.of(teamId)))
            .build();

        final String serviceId = relationManger.addEntity(
            service1,
            EntityDescriptor.SERVICE,
            ImmutableList.of(new TargetReferences(EntityDescriptor.TEAM, teamId))).get();

        final Entity teamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity teamEntityOtherName = Entity
            .builder(teamEntity)
            .property(Constants.NAME, "team11")
            .property(EntityDescriptor.TEAM.ids(), ImmutableList.of("user1", "user2"))
            .build();

        relationManger.updateEntity(
            datastoreWrapper.txn(),
            teamEntityOtherName,
            EntityDescriptor.TEAM,
            ImmutableList.of(
                new TargetReferences(EntityDescriptor.USER, "user1"),
                new TargetReferences(EntityDescriptor.USER, "user2")),
            ImmutableList.of(),
            true).get();

        final Entity storedTeamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity storedUser1Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();

        final Entity storedUser2Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user2"))
            .getEntity();

        final Entity storedServiceEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.SERVICE.kind(), serviceId))
            .getEntity();


        assertEquals(
            ImmutableMap.of("user1", "user1", "user2", "user2"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.USER.names()));

        assertEquals(
            ImmutableList.of("user1", "user2"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.USER.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team11"),
            Common.getStringMap(storedUser1Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUser1Entity, EntityDescriptor.TEAM.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team11"),
            Common.getStringMap(storedUser2Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUser2Entity, EntityDescriptor.TEAM.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team11"),
            Common.getStringMap(storedServiceEntity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedServiceEntity, EntityDescriptor.TEAM.ids()));

    }

    @Test
    public void testAddUpdateTeamRemoveUser() throws Exception {
        final Entity team1 = Entity
            .builder(EntityDescriptor.TEAM.kind(), "team1")
            .property("name", "team1")
            .property(EntityDescriptor.USER.ids(), Common.toList(ImmutableList.of("user1")))
            .build();

        final String teamId = relationManger.addEntity(
            team1,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user1"))).get();

        final Entity teamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity teamEntityRemoveUser = Entity
            .builder(teamEntity)
            .property(EntityDescriptor.TEAM.ids(), ImmutableList.of("user2"))
            .build();

        relationManger.updateEntity(
            datastoreWrapper.txn(),
            teamEntityRemoveUser,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user2")),
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user1")),
            false).get();

        final Entity storedTeamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity storedUser1Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();

        final Entity storedUser2Entity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user2"))
            .getEntity();


        assertEquals(
            ImmutableMap.of("user2", "user2"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.USER.names()));

        assertEquals(
            ImmutableList.of("user2"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.USER.ids()));

        assertEquals(
            ImmutableMap.of(),
            Common.getStringMap(storedUser1Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(),
            Common.getStringList(storedUser1Entity, EntityDescriptor.TEAM.ids()));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedUser2Entity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUser2Entity, EntityDescriptor.TEAM.ids()));

    }

    @Test
    public void testReverseReference() throws Exception {
        final Entity team1 = Entity
            .builder(EntityDescriptor.TEAM.kind(), "team1")
            .property("name", "team1")
            .property(EntityDescriptor.USER.ids(), Common.toList(ImmutableList.of("user1")))
            .build();

        final String teamId = relationManger.addEntity(
            team1,
            EntityDescriptor.TEAM,
            ImmutableList.of(new TargetReferences(EntityDescriptor.USER, "user1"))).get();

        final Entity updatedUserEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();

        final Entity modifiedUserEntity = Entity
            .builder(updatedUserEntity)
            .property(Constants.NAME, "user11")
            .build();

        relationManger.updateReverseEntity(
            datastoreWrapper.txn(),
            modifiedUserEntity,
            EntityDescriptor.USER).get();

        final Entity storedTeamEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.TEAM.kind(), teamId))
            .getEntity();

        final Entity storedUserEntity = datastore
            .execute(QueryBuilder.query(EntityDescriptor.USER.kind(), "user1"))
            .getEntity();


        assertEquals(
            ImmutableMap.of("user1", "user11"),
            Common.getStringMap(storedTeamEntity, EntityDescriptor.USER.names()));

        assertEquals(
            ImmutableList.of("user1"),
            Common.getStringList(storedTeamEntity, EntityDescriptor.USER.ids()));

        assertEquals("user11", storedUserEntity.getString(Constants.NAME));

        assertEquals(
            ImmutableMap.of(teamId, "team1"),
            Common.getStringMap(storedUserEntity, EntityDescriptor.TEAM.names()));

        assertEquals(
            ImmutableList.of(teamId),
            Common.getStringList(storedUserEntity, EntityDescriptor.TEAM.ids()));
    }

    private DatastoreConfig datastoreConfig() {
        final DatastoreConfig.Builder config = DatastoreConfig
            .builder()
            .connectTimeout(5000)
            .requestTimeout(1000)
            .maxConnections(5)
            .requestRetry(3)
            .host(DATASTORE_HOST)
            .dataset(DATASET_ID);

        if (NAMESPACE != null) {
            config.namespace(NAMESPACE);
        }

        return config.build();
    }

    private void resetDatastore() throws Exception {
        for (EntityDescriptor descriptor : EntityDescriptor.values()) {
            removeAll(descriptor.kind());
        }
    }

    private void removeAll(final String kind) throws Exception {
        final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
        for (final Entity entity : datastore.execute(queryAll)) {
            datastore.execute(QueryBuilder.delete(entity.getKey()));
        }
    }
}