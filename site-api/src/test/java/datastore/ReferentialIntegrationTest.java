package datastore;

import com.spotify.asyncdatastoreclient.*;
import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.api.exceptions.ReferentialIntegrityException;
import io.vigilante.site.api.impl.datastore.DatastoreManager;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;

public class ReferentialIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(30000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8080");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	public static final int TIMEOUT = 5;

	protected static Datastore datastore;

	protected static UserManager userManager;
	protected static TeamManager teamManager;
	protected static ScheduleManager scheduleManager;

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
		scheduleManager = manager.getScheduleManager();

		userA = User.newBuilder().setName("foo").setEmail("foo@foo.org").setTimeZone("UTC").build();
		userB = User.newBuilder().setName("bar").setEmail("bar@foo.org").setTimeZone("UTC+1").build();

		teamA = Team.newBuilder().setName("teamA").build();
		teamB = Team.newBuilder().setName("teamB").build();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDeleteUserInTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();
		try {
			userManager.deleteUser(NAMESPACE, uid).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ReferentialIntegrityException) {
				return;
			}
		}
		fail("ReferentialIntegrityException expected");
	}

	@Test
	public void testDeleteUserInSchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, teamA).get();

		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid, userA, tid, teamA);

		scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		try {
			userManager.deleteUser(NAMESPACE, uid).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ReferentialIntegrityException) {
				return;
			}
		}

		fail("ReferentialIntegrityException expected");
	}

	@Test
	public void testDeleteTeamInSchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, teamA).get();

		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid, userA, tid, teamA);

		scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		try {
			teamManager.deleteTeam(NAMESPACE, tid).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ReferentialIntegrityException) {
				return;
			}
		}

		fail("ReferentialIntegrityException expected");
	}

	@Test
	public void testUserDoesntExistInTeam() throws InterruptedException, ExecutionException, TimeoutException {
		TeamOperations teamOperations = new TeamOperations(datastore);
		long tid = teamOperations.addTeam(NAMESPACE, addUser(teamA, addId(userA, 1))).get();
		teamManager.getTeam(NAMESPACE, tid).get();
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
		removeAll("schedules");
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

	private Team addUser(Team team, User user) {
		return team.toBuilder().addUsers(user).build();
	}
}
