package datastore;

import com.spotify.asyncdatastoreclient.*;
import io.vigilante.TeamProtos.Team;
import io.vigilante.TeamProtos.Teams;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.api.impl.datastore.DatastoreManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TeamIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(30000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8080");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	public static final int TIMEOUT = 5;

	protected static Datastore datastore;

	protected static UserManager userManager;
	protected static TeamManager teamManager;

	protected static Executor executor;

	protected static User userA;
	protected static User userB;

	protected static Team teamA;
	protected static Team teamB;

	@Before
	public void setUp() throws Exception {
		datastore = Datastore.create(datastoreConfig());
		resetDatastore();

		DatastoreManager manager = new DatastoreManager(datastore);

		userManager = manager.getUserManager();
		teamManager = manager.getTeamManager();

		userA = User.newBuilder().setName("foo").setEmail("foo@foo.org").setTimeZone("UTC").build();
		userB = User.newBuilder().setName("bar").setEmail("bar@foo.org").setTimeZone("UTC+1").build();

		teamA = Team.newBuilder().setName("teamA").build();
		teamB = Team.newBuilder().setName("teamB").build();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddBasicTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Team team = teamManager.getTeam(NAMESPACE, tid).get();

		assertEquals(team, addUserAndIds(teamA, tid, userA, uid));
	}

	@Test
	public void testModifyBasicTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long uid2 = userManager.addUser(NAMESPACE, userB).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		teamManager.updateTeam(NAMESPACE, tid, addUser(teamB, addId(userB, uid2))).get();

		Team team = teamManager.getTeam(NAMESPACE, tid).get();

		assertEquals(team, addUserAndIds(teamB, tid, userB, uid2));
	}

	@Test
	public void testGetTeams() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUserId(teamA, uid)).get();

		Teams teams = teamManager.getTeams(NAMESPACE).get();

		assertEquals(teams, Teams.newBuilder().addTeams(addId(teamA, tid)).build());
	}

	@Test
	public void testGetTeamsForUser() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long uid2 = userManager.addUser(NAMESPACE, userB).get();
		teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		long tid2 = teamManager.addTeam(NAMESPACE, addUser(teamB, addId(userA, uid2))).get();

		Teams teams = teamManager.getTeamsForUser(NAMESPACE, uid2).get();

		assertEquals(1, teams.getTeamsCount());
		assertEquals(tid2, teams.getTeams(0).getId());
	}

	@Test
	public void testDeleteTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long id = teamManager.addTeam(NAMESPACE, teamA).get();
		teamManager.deleteTeam(NAMESPACE, id).get();

		for (int i = 0; i < 10; i++) { /* consistency... */
			Teams teams = teamManager.getTeams(NAMESPACE).get();
			if (teams.getTeamsCount() == 0) {
				return;
			}
		}

		fail("team wasn't deleted");
	}

	private DatastoreConfig datastoreConfig() {
		final DatastoreConfig.Builder config = DatastoreConfig.builder()
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
		removeAll("user");
		removeAll("notification");
		removeAll("team");
	}

	private void removeAll(final String kind) throws Exception {
		final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
		for (final Entity entity : datastore.execute(queryAll)) {
			datastore.execute(QueryBuilder.delete(entity.getKey()));
		}
	}

	private User addId(final User user, final long id) {
		return user.toBuilder().setId(id).build();
	}

	private Team addId(final Team team, final long id) {
		return team.toBuilder().setId(id).build();
	}

	private Team addUser(Team team, User user) {
		return team.toBuilder().addUsers(user).build();
	}

	private Team addUserId(Team team, Long uid) {
		return addUser(team, User.newBuilder().setId(uid).build());
	}

	private Team addUserAndIds(Team team, Long tid, User user, Long uid) {
		return addId(addUser(team, addId(user.toBuilder().build(), uid)), tid);
	}
}
