package datastore;

import com.spotify.asyncdatastoreclient.*;
import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.ServiceProtos.Services;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.api.ServiceManager;
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
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ServiceIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(30000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8080");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	public static final int TIMEOUT = 5;

	protected static Datastore datastore;

	protected static UserManager userManager;
	protected static ServiceManager serviceManager;
	protected static TeamManager teamManager;
	protected static ScheduleManager scheduleManager;

	protected static Executor executor;

	protected static User userA;
	protected static User userB;

	protected static Team teamA;
	protected static Team teamB;

	protected static Schedule scheduleA;
	protected static Schedule scheduleB;

	@Before
	public void setUp() throws Exception {
		datastore = Datastore.create(datastoreConfig());
		resetDatastore();

		DatastoreManager manager = new DatastoreManager(datastore);

		userManager = manager.getUserManager();
		serviceManager = manager.getServiceManager();
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
	public void testAddBasicService() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid, userA, tid, teamA);
		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Service serviceA = buildService("serviceA", sid, scheduleA, tid, teamA);
		Long serviceId = serviceManager.addService(NAMESPACE, serviceA).get();

		Service service = serviceManager.getService(NAMESPACE, serviceId).get();
		assertTrue(service.hasKey());

		assertEquals(addId(serviceA, serviceId), removeKey(service));
	}

	@Test
	public void testModifyBasicService() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long tid1 = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid1, userA, tid1, teamA);
		Long sid1 = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Long uid2 = userManager.addUser(NAMESPACE, userB).get();
		Long tid2 = teamManager.addTeam(NAMESPACE, addUser(teamB, addId(userB, uid2))).get();
		Schedule scheduleB = ScheduleIntegrationTest.buildScheduleB(uid2, userB, tid2, teamB);
		Long sid2 = scheduleManager.addSchedule(NAMESPACE, scheduleB).get();

		Service serviceA = buildService("serviceA", sid1, scheduleA, tid1, teamA);
		Long serviceId = serviceManager.addService(NAMESPACE, serviceA).get();

		Service serviceB = buildService("serviceB", sid2, scheduleB, tid2, teamB);
		serviceManager.updateService(NAMESPACE, serviceId, serviceB).get();

		Service service = serviceManager.getService(NAMESPACE, serviceId).get();
		assertTrue(service.hasKey());

		assertEquals(addId(serviceB, serviceId), removeKey(service));
	}

	@Test
	public void testGetServices() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid, userA, tid, teamA);
		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Service serviceA = buildService("serviceA", sid, scheduleA, tid, teamA);
		Long serviceId = serviceManager.addService(NAMESPACE, serviceA).get();

		Services services = Services.newBuilder()
				.addAllServices(serviceManager
						.getServices(NAMESPACE)
						.get()
						.getServicesList()
						.stream()
						.map(s -> removeKey(s)).collect(Collectors.toList()))
				.build();

		Service expected = addId(serviceA, serviceId).toBuilder().setTeam(Team.newBuilder().setId(tid).build())
				.setSchedule(Schedule.newBuilder().setId(sid)).build();
		assertEquals(Services.newBuilder().addServices(expected).build(), services);
	}

	@Test
	public void testGetServicesForTeam() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long tid1 = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid1, userA, tid1, teamA);
		Long sid1 = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Long uid2 = userManager.addUser(NAMESPACE, userB).get();
		Long tid2 = teamManager.addTeam(NAMESPACE, addUser(teamB, addId(userB, uid2))).get();
		Schedule scheduleB = ScheduleIntegrationTest.buildScheduleB(uid2, userB, tid2, teamB);
		Long sid2 = scheduleManager.addSchedule(NAMESPACE, scheduleB).get();

		Service serviceA = buildService("serviceA", sid1, scheduleA, tid1, teamA);
		long serviceAId = serviceManager.addService(NAMESPACE, serviceA).get();
		Service serviceB = buildService("serviceB", sid2, scheduleB, tid2, teamB);
		long serviceBId = serviceManager.addService(NAMESPACE, serviceB).get();

		assertEquals(1, serviceManager.getServicesForTeam(NAMESPACE, tid1).get().getServicesCount());
		assertEquals(serviceAId, serviceManager.getServicesForTeam(NAMESPACE, tid1).get().getServices(0).getId());
		assertEquals(1, serviceManager.getServicesForTeam(NAMESPACE, tid2).get().getServicesCount());
		assertEquals(serviceBId, serviceManager.getServicesForTeam(NAMESPACE, tid2).get().getServices(0).getId());
	}

	@Test
	public void testGetServicesForSchedule() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid1 = userManager.addUser(NAMESPACE, userA).get();
		Long tid1 = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid1))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid1, userA, tid1, teamA);
		Long sid1 = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Long uid2 = userManager.addUser(NAMESPACE, userB).get();
		Long tid2 = teamManager.addTeam(NAMESPACE, addUser(teamB, addId(userB, uid2))).get();
		Schedule scheduleB = ScheduleIntegrationTest.buildScheduleB(uid2, userB, tid2, teamB);
		Long sid2 = scheduleManager.addSchedule(NAMESPACE, scheduleB).get();

		Service serviceA = buildService("serviceA", sid1, scheduleA, tid1, teamA);
		long serviceAId = serviceManager.addService(NAMESPACE, serviceA).get();
		Service serviceB = buildService("serviceB", sid2, scheduleB, tid2, teamB);
		long serviceBId = serviceManager.addService(NAMESPACE, serviceB).get();

		assertEquals(1, serviceManager.getServicesForSchedule(NAMESPACE, sid1).get().getServicesCount());
		assertEquals(serviceAId, serviceManager.getServicesForSchedule(NAMESPACE, sid1).get().getServices(0).getId());
		assertEquals(1, serviceManager.getServicesForSchedule(NAMESPACE, sid2).get().getServicesCount());
		assertEquals(serviceBId, serviceManager.getServicesForSchedule(NAMESPACE, sid2).get().getServices(0).getId());
	}

	@Test
	public void testDeleteService() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long tid = teamManager.addTeam(NAMESPACE, addUser(teamA, addId(userA, uid))).get();
		Schedule scheduleA = ScheduleIntegrationTest.buildScheduleA(uid, userA, tid, teamA);
		Long sid = scheduleManager.addSchedule(NAMESPACE, scheduleA).get();

		Service serviceA = buildService("serviceA", sid, scheduleA, tid, teamA);
		Long serviceId = serviceManager.addService(NAMESPACE, serviceA).get();
		serviceManager.deleteService(NAMESPACE, serviceId).get();

		for (int i = 0; i < 10; i++) { /* consistency... */
			Services services = serviceManager.getServices(NAMESPACE).get();
			if (services.getServicesCount() == 0) {
				return;
			}
		}

		fail("service wasn't deleted");
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
		removeAll("service");
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

	private Service addId(Service service, Long id) {
		return service.toBuilder().setId(id)
				.setSchedule(service.getSchedule().toBuilder().clearScheduleLevels().clearTeam().build()).build();
	}

	private Service buildService(String name, Long sid, Schedule schedule, Long tid, Team team) {
		return Service.newBuilder().setName(name).setSchedule(schedule.toBuilder().setId(sid).build())
				.setTeam(team.toBuilder().setId(tid).build()).build();
	}

	private Service removeKey(Service service) {
		return service.toBuilder().clearKey().build();
	}
}
