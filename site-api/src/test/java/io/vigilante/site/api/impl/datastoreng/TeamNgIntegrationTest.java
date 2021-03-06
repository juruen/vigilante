package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreConfig;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.Team;
import io.viglante.common.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class TeamNgIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(300000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8081");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	protected static Datastore datastore;
	protected static DatastoreUserManager userManager;
	protected static DatastoreTeamManager teamManager;


	protected static Executor executor;

	protected static User userA;
	protected static User userB;

	@Before
	public void setUp() throws Exception {
		datastore = Datastore.create(datastoreConfig());
		resetDatastore();

		DatastoreWrapper datastoreWrapper = new DatastoreWrapper(datastore);

		final Relations relations = Relations.builder()
			.addRelation(EntityDescriptor.TEAM, EntityDescriptor.USER)
			.build();

		RelationManager relationManger = new RelationManager(relations, datastoreWrapper);

		DatastoreHandler handler = new DatastoreHandler(datastoreWrapper, relationManger);

		userManager = new DatastoreUserManager(handler);

		teamManager = new DatastoreTeamManager(handler);

		userA = User
			.builder()
			.name("foo")
			.email("foo@foo.org")
			.timeZone("UTC")
			.notifications(ImmutableList.of())
			.build();

		userB = User
			.builder()
			.name("bar")
			.email("bar@bar.org")
			.timeZone("UTC+1")
			.notifications(ImmutableList.of())
			.build();

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddTeam() throws Exception{
		final String userId = userManager.addUser(NAMESPACE, userA).get();
		final Team team = Team.builder().name("team A").users(ImmutableList.of(userId)).build();
		final String teamId = teamManager.addTeam(NAMESPACE, team).get();
		final Team addedTeam = teamManager.getTeam(NAMESPACE, teamId).get();

		assertEquals("team A", addedTeam.getName());
    }

	@Test
	public void testUpdateTeam() throws Exception {
		final String userAId = userManager.addUser(NAMESPACE, userA).get();
		final String userBId = userManager.addUser(NAMESPACE, userB).get();

		final Team team = Team.builder().name("team A").build();
		final String teamId = teamManager.addTeam(NAMESPACE, team).get();
		teamManager.getTeam(NAMESPACE, teamId).get();

		final Team updateTeam1 = Team
			.builder()
			.id(Optional.empty())
			.name("team B")
			.users(ImmutableList.of(userAId))
			.build();

		teamManager.updateTeam(NAMESPACE, teamId, updateTeam1).get();

		final Team fetchedUpdateTeam1 = teamManager.getTeam(NAMESPACE, teamId).get();

        final Team updateTeam2 = Team
			.builder()
			.id(Optional.empty())
			.name("team B")
			.users(ImmutableList.of(userBId))
			.build();

        teamManager.updateTeam(NAMESPACE, teamId, updateTeam2).get();

        final Team fetchedUpdateTeam2 = teamManager.getTeam(NAMESPACE, teamId).get();

        assertEquals("team B", fetchedUpdateTeam1.getName());
        assertEquals(ImmutableList.of(userAId), fetchedUpdateTeam1.getUsers());
		assertEquals("team B", fetchedUpdateTeam2.getName());
		assertEquals(ImmutableList.of(userBId), fetchedUpdateTeam2.getUsers());
	}

	@Test(expected = SiteExternalException.class)
	public void testDeleteTeamWithUser() throws Throwable {
		final String userId = userManager.addUser(NAMESPACE, userA).get();

		final Team team = Team.builder().name("team A").build();
		final String teamId = teamManager.addTeam(NAMESPACE, team).get();

		final Team updateTeam = Team
			.builder()
			.id(Optional.empty())
			.name("team B")
			.users(ImmutableList.of(userId))
			.build();

		teamManager.updateTeam(NAMESPACE, teamId, updateTeam).get();

        teamManager.deleteTeam(NAMESPACE, teamId).get();

        // Should throw exception
        try {
            teamManager.getTeam(NAMESPACE, teamId).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
	}

    @Test(expected = SiteExternalException.class)
    public void testDeleteUserInTeam() throws Throwable {
        final String userAId = userManager.addUser(NAMESPACE, userA).get();
        final String userBId = userManager.addUser(NAMESPACE, userB).get();

        final Team team = Team.builder().name("team A").build();
        final String teamId = teamManager.addTeam(NAMESPACE, team).get();
        teamManager.getTeam(NAMESPACE, teamId).get();

        final Team updateTeam1 = Team
            .builder()
            .id(Optional.empty())
            .name("team B")
            .users(ImmutableList.of(userAId))
            .build();

        teamManager.updateTeam(NAMESPACE, teamId, updateTeam1).get();

        try {
            userManager.deleteUser(NAMESPACE, userAId).get();
            fail("expected ReferentialIntegrityException");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ReferentialIntegrityException.class));
        }

        final Team updateTeam2 = Team
            .builder()
            .id(Optional.of(teamId))
            .name("team B")
            .users(ImmutableList.of(userBId))
            .build();

        teamManager.updateTeam(NAMESPACE, teamId, updateTeam2).get();

        final Team fetchedUpdateTeam2 = teamManager.getTeam(NAMESPACE, teamId).get();

        assertEquals("team B", fetchedUpdateTeam2.getName());
        assertEquals(ImmutableList.of(userBId), fetchedUpdateTeam2.getUsers());

        try {
            userManager.deleteUser(NAMESPACE, userBId).get();
            fail("expected ReferentialIntegrityException");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ReferentialIntegrityException.class));
        }

        userManager.deleteUser(NAMESPACE, userAId).get();

        // Should throw exception
        try {
			userManager.getUser(NAMESPACE, userAId).get();
		} catch (ExecutionException e) {
			throw e.getCause();
		}
    }



	@Test
    public void testGetTeams() throws Exception {
        final String userAId = userManager.addUser(NAMESPACE, userA).get();
        final String userBId = userManager.addUser(NAMESPACE, userB).get();

        final Team teamA = Team.builder().name("team A").build();
        final String teamAId = teamManager.addTeam(NAMESPACE, teamA).get();

		final Team updateTeamA = Team
            .builder()
            .id(Optional.of(teamAId))
            .name("team A")
            .users(ImmutableList.of(userAId))
            .build();

        teamManager.updateTeam(NAMESPACE, teamAId, updateTeamA).get();

        final Team teamB = Team.builder().name("team B").build();
        final String teamBId = teamManager.addTeam(NAMESPACE, teamB).get();

        final Team updateTeamB = Team
            .builder()
            .id(Optional.of(teamBId))
            .name("team B")
            .users(ImmutableList.of(userBId))
            .build();

        teamManager.updateTeam(NAMESPACE, teamBId, updateTeamB).get();

        final Team teamC = Team.builder().name("team C").build();
        final String teamCId = teamManager.addTeam(NAMESPACE, teamC).get();
        final Team updateTeamC = Team
            .builder()
            .id(Optional.of(teamCId))
            .name("team C")
            .users(ImmutableList.of())
            .build();

        final List<Team> teams = teamManager.getTeams(NAMESPACE).get();

        assertEquals(ImmutableList.of(updateTeamA, updateTeamB, updateTeamC), teams);
    }

	private DatastoreConfig datastoreConfig() {
		final DatastoreConfig.Builder config = DatastoreConfig
            .builder()
            .connectTimeout(5000)
            .requestTimeout(10000)
            .maxConnections(10)
            .requestRetry(3)
            .host(DATASTORE_HOST)
            .dataset(DATASET_ID);

		if (NAMESPACE != null) {
			config.namespace(NAMESPACE);
		}

		return config.build();
	}

	private void resetDatastore() throws Exception {
		removeAll(Constants.USER_KIND);
        removeAll(Constants.TEAM_KIND);
	}

	private void removeAll(final String kind) throws Exception {
		final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
		for (final Entity entity : datastore.execute(queryAll)) {
			datastore.execute(QueryBuilder.delete(entity.getKey()));
		}
	}
}
