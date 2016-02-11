package datastore;

import com.spotify.asyncdatastoreclient.*;
import io.vigilante.UserProtos.User;
import io.vigilante.UserProtos.User.NotificationRule;
import io.vigilante.UserProtos.User.NotificationType;
import io.vigilante.UserProtos.Users;
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

public class UserIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(3000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8080");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	public static final int TIMEOUT = 5;

	protected static Datastore datastore;
	protected static UserManager userManager;

	protected static Executor executor;

	protected static User userA;
	protected static User userB;

	protected static NotificationRule notificationA;
	protected static NotificationRule notificationB;

	@Before
	public void setUp() throws Exception {
		datastore = Datastore.create(datastoreConfig());
		resetDatastore();

		userManager = new DatastoreManager(datastore).getUserManager();

		userA = User.newBuilder().setName("foo").setEmail("foo@foo.org").setTimeZone("UTC").build();
		userB = User.newBuilder().setName("bar").setEmail("bar@foo.org").setTimeZone("UTC+1").build();

		notificationA = NotificationRule.newBuilder().setType(NotificationType.EMAIL).setContactAlias("my email")
				.setContactAddress("foo@foo.org").setStart(0).build();

		notificationB = NotificationRule.newBuilder().setType(NotificationType.PHONE).setContactAlias("my phone")
				.setStart(1).setContactAddress("+0011223344").build();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddBasicUser() throws InterruptedException, ExecutionException, TimeoutException {
		Long id = userManager.addUser(NAMESPACE, userA).get();
		User user = userManager.getUser(NAMESPACE, id).get();

		assertEquals(user, addId(userA, id));
	}

	@Test
	public void testModifyBasicUser() throws InterruptedException, ExecutionException, TimeoutException {
		Long id = userManager.addUser(NAMESPACE, userA).get();
		userManager.updateUser(NAMESPACE, id, userB).get();

		User user = userManager.getUser(NAMESPACE, id).get();

		assertEquals(user, addId(userB, id));
	}

	@Test
	public void testAddCompleteUser() throws InterruptedException, ExecutionException, TimeoutException {
		User createdUser = userA.toBuilder().addNotifications(notificationA).build();

		Long id = userManager.addUser(NAMESPACE, createdUser).get();
		User user = userManager.getUser(NAMESPACE, id).get();

		long nid = user.getNotifications(0).getId();
		User expected = userA.toBuilder().addNotifications(addId(notificationA, nid)).setId(id).build();

		assertEquals(expected, user);
	}

	@Test
	public void testModifyCompleteUser() throws InterruptedException, ExecutionException, TimeoutException {
		User createdUser = userA.toBuilder().addNotifications(notificationA).build();
		User modifiedUser = userB.toBuilder().addNotifications(notificationB).build();

		Long id = userManager.addUser(NAMESPACE, createdUser).get();

		userManager.updateUser(NAMESPACE, id, modifiedUser).get();

		User user = userManager.getUser(NAMESPACE, id).get();

		long nid = user.getNotifications(0).getId();
		User expected = userB.toBuilder().addNotifications(addId(notificationB, nid)).setId(id).build();

		assertEquals(expected, user);
	}

	@Test
	public void testAddNotification() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long nid = userManager.addUserNotification(NAMESPACE, uid, notificationA).get();
		User user = userManager.getUser(NAMESPACE, uid).get();

		assertEquals(user, userA.toBuilder().setId(uid).addNotifications(addId(notificationA, nid)).build());
	}

	@Test
	public void testModifyNotification() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long nid = userManager.addUserNotification(NAMESPACE, uid, notificationA).get();
		userManager.updateUserNotification(NAMESPACE, uid, nid, notificationB).get();
		User user = userManager.getUser(NAMESPACE, uid).get();

		assertEquals(user, userA.toBuilder().setId(uid).addNotifications(addId(notificationB, nid)).build());
	}

	@Test
	public void testDeleteNotification() throws InterruptedException, ExecutionException, TimeoutException {
		Long uid = userManager.addUser(NAMESPACE, userA).get();
		Long nid = userManager.addUserNotification(NAMESPACE, uid, notificationA).get();
		userManager.deleteUserNotification(NAMESPACE, uid, nid).get();
		User user = userManager.getUser(NAMESPACE, uid).get();

		assertEquals(user, addId(userA, uid));
	}

	@Test
	public void testGetUsers() throws InterruptedException, ExecutionException, TimeoutException {
		Long id = userManager.addUser(NAMESPACE, userA).get();

		Users users = userManager.getUsers(NAMESPACE).get();

		assertEquals(users, Users.newBuilder().addUsers(addId(userA, id)).build());
	}

	@Test
	public void testDeleteUser() throws InterruptedException, ExecutionException, TimeoutException {
		Long id = userManager.addUser(NAMESPACE, userA).get();
		userManager.deleteUser(NAMESPACE, id);

		for (int i = 0; i < 10; i++) { /* consistency... */
			Users users = userManager.getUsers(NAMESPACE).get();
			if (users.getUsersCount() == 0) {
				return;
			}
		}

		fail("user wasn't deleted");
	}

	private DatastoreConfig datastoreConfig() {
		final DatastoreConfig.Builder config = DatastoreConfig.builder().connectTimeout(5000).requestTimeout(1000)
				.maxConnections(5).requestRetry(3).host(DATASTORE_HOST).dataset(DATASET_ID);

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

	private NotificationRule addId(final NotificationRule notification, final long id) {
		return notification.toBuilder().setId(id).build();
	}
}
