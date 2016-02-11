package datastore;

import com.spotify.asyncdatastoreclient.*;
import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ScheduleProtos.Schedule.ScheduleLevel;
import io.vigilante.ScheduleProtos.Schedule.TimeRange;
import io.vigilante.ScheduleProtos.Schedules;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.ScheduleManager;
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

public class ScheduleIntegrationTest {

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
	public void testAddSchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Schedule scheduleA = buildScheduleA(uid, userA, tid, teamA);

		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Schedule schedule = scheduleManager.getSchedule(NAMESPACE, sid).get();

		assertEquals(addId(scheduleA, sid), schedule);
	}

	@Test
	public void testModifySchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long uid2 = userManager.addUser(NAMESPACE, userB).get();

		Long tid1 = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		Long tid2 = teamManager.addTeam(NAMESPACE, addUser(teamB, addId(userB, uid2))).get();

		Schedule scheduleA = buildScheduleA(uid1, userA, tid1, teamA);
		Schedule scheduleB = buildScheduleB(uid2, userB, tid2, teamB);

		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		scheduleManager.updateSchedule(NAMESPACE, sid, scheduleB).get();

		Schedule schedule = scheduleManager.getSchedule(NAMESPACE, sid).get();

		assertEquals(addId(scheduleB, sid), schedule);
	}

	@Test
	public void testGetSchedules() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Schedule scheduleA = buildScheduleA(uid, userA, tid, teamA);

		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Schedules schedules = scheduleManager.getSchedules(NAMESPACE).get();

		Schedules expected = Schedules
				.newBuilder()
				.addSchedules(
						removeLevels(addId(scheduleA, sid)).toBuilder().setTeam(Team.newBuilder().setId(tid).build()))
				.build();
		assertEquals(expected, schedules);
	}

	@Test
	public void testDeleteSchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Schedule scheduleA = buildScheduleA(uid, userA, tid, teamA);

		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		scheduleManager.deleteSchedule(NAMESPACE, sid);

		for (int i = 0; i < 10; i++) { /* consistency... */
			Schedules schedules = scheduleManager.getSchedules(NAMESPACE).get();
			if (schedules.getSchedulesCount() == 0) {
				return;
			}
		}

		fail("schedule wasn't deleted");
	}

	@Test
	public void testGetSchedulesForUser() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Schedule scheduleA = buildScheduleA(uid, userA, tid, teamA);

		long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Schedules schedules = scheduleManager.getSchedulesForUser(NAMESPACE, uid).get();

		assertEquals(sid, schedules.getSchedules(0).getId());
	}

	@Test
	public void testGetSchedulesForTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();

		Schedule scheduleA = buildScheduleA(uid, userA, tid, teamA);

		long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Schedules schedules = scheduleManager.getSchedulesForTeam(NAMESPACE, tid).get();

		assertEquals(sid, schedules.getSchedules(0).getId());
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
		removeAll("schedule");
	}

	private void removeAll(final String kind) throws Exception {
		final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
		for (final Entity entity : datastore.execute(queryAll)) {
			datastore.execute(QueryBuilder.delete(entity.getKey()));
		}
	}

	private static User addId(final User user, final long id) {
		return user.toBuilder().setId(id).build();
	}

	private static Schedule addId(final Schedule schedule, final long id) {
		return schedule.toBuilder().setId(id).build();
	}

	private static Team addUser(Team team, User user) {
		return team.toBuilder().addUsers(user).build();
	}

	private static Schedule removeLevels(Schedule schedule) {
		return schedule.toBuilder().clearScheduleLevels().build();
	}

	public static Schedule buildScheduleA(final long uid, final User user, final long tid, final Team team) {
		return Schedule
				.newBuilder()
				.setName("schedule A")
				.setLength(10)
				.setStart(0)
				.setTeam(team.toBuilder().setId(tid).clearUsers().build())
				.addScheduleLevels(
						ScheduleLevel
								.newBuilder()
								.setLevel(0)
								.addTimeRanges(
										TimeRange.newBuilder().setUser(addId(user, uid)).setLength(0).setStart(0)
												.build()))
				.build();
	}

	public static Schedule buildScheduleB(final long uid, final User user, final long tid, final Team team) {

		return Schedule
				.newBuilder()
				.setName("schedule B")
				.setLength(12)
				.setStart(2)
				.setTeam(team.toBuilder().setId(tid).clearUsers().build())
				.addScheduleLevels(
						ScheduleLevel
								.newBuilder()
								.setLevel(0)
								.addTimeRanges(
										TimeRange.newBuilder().setUser(addId(user, uid)).setLength(1).setStart(1)
												.build()))
				.build();
	}
}
