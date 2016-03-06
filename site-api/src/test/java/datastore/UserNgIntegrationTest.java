package datastore;

import com.google.common.collect.ImmutableList;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreConfig;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastoreng.DatastoreUserManager;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.viglante.common.model.User;
import io.viglante.common.model.User.NotificationRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class UserNgIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(3000);

	public static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8081");
	public static final String DATASET_ID = System.getProperty("dataset", "test");
	public static final String NAMESPACE = System.getProperty("namespace", "test");

	public static final int TIMEOUT = 5;

	protected static Datastore datastore;
	protected static DatastoreUserManager userManager;

	protected static Executor executor;

	protected static User userA;
	protected static User userB;

	protected static NotificationRule notificationA;
	protected static NotificationRule notificationB;

	@Before
	public void setUp() throws Exception {
		datastore = Datastore.create(datastoreConfig());
		resetDatastore();

		//userManager new DatastoreUserManager(new DatastoreWrapper(datastore));


		notificationA = NotificationRule.builder()
			.type(NotificationRule.NotificationType.EMAIL)
			.contactAlias("my email")
			.contactAddress("foo@foo.org")
            .contactDetails(Optional.empty())
			.start(0)
			.build();

		notificationB = NotificationRule.builder()
			.type(NotificationRule.NotificationType.PHONE)
			.contactAlias("my phone")
			.contactAddress("+0011223344")
            .contactDetails(Optional.empty())
            .start(1)
			.build();

		userA = User
			.builder()
			.name("foo")
			.email("foo@foo.org")
			.timeZone("UTC")
			.notifications(ImmutableList.of(notificationA))
			.build();

		userB = User
			.builder()
			.name("bar")
			.email("bar@bar.org")
			.timeZone("UTC+1")
			.notifications(ImmutableList.of(notificationB))
			.build();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddUser() throws InterruptedException, ExecutionException, TimeoutException {
		final String id = userManager.addUser(NAMESPACE, userA).get();
		final User user = userManager.getUser(NAMESPACE, id).get();

        assertEquals(addUserId(userA, id), user);
    }

    @Test
	public void testUpdateUser() throws InterruptedException, ExecutionException, TimeoutException {
        final String id = userManager.addUser(NAMESPACE, userA).get();
        userManager.updateUser(NAMESPACE, id, userB).get();

        final User user = userManager.getUser(NAMESPACE, id).get();

        assertEquals(addUserId(userB, id), user);
    }

    @Test(expected = SiteExternalException.class)
    public void testDeleteUser() throws Throwable {
        final String id = userManager.addUser(NAMESPACE, userA).get();
        userManager.deleteUser(NAMESPACE, id);

        try {
            userManager.getUser(NAMESPACE, id).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testGetUsers() throws InterruptedException, ExecutionException, TimeoutException {
        final String idA = userManager.addUser(NAMESPACE, userA).get();
        final String idB = userManager.addUser(NAMESPACE, userB).get();
        final List<User> users = userManager.getUsers(NAMESPACE).get();

        assertEquals(ImmutableList.of(addUserId(userB, idB), addUserId(userA, idA)), users);
    }

    private User addUserId(User user, String id) {
        return user.copyBuilder().id(Optional.of(id)).build();
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
		removeAll(Constants.USER_KIND);
	}

	private void removeAll(final String kind) throws Exception {
		final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
		for (final Entity entity : datastore.execute(queryAll)) {
			datastore.execute(QueryBuilder.delete(entity.getKey()));
		}
	}
}
