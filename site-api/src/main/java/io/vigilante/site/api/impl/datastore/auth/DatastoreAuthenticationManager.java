package io.vigilante.site.api.impl.datastore.auth;

import static com.spotify.asyncdatastoreclient.QueryBuilder.eq;
import io.vigilante.UserProtos.User;
import io.vigilante.site.api.AuthenticationManager;
import io.vigilante.site.api.AuthenticationResponse;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.vigilante.site.impl.datastore.basic.user.UserQueries;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreException;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.QueryResult;

@Slf4j
public class DatastoreAuthenticationManager implements AuthenticationManager {
	private final static String CLIENT_ID = "165438270066-1rnj2b4mbfuabd3evlkidjsvtaugj76d.apps.googleusercontent.com";

	private SecureRandom random = new SecureRandom();

	private static final String DEFAULT_TIME_ZONE = "UTC";

	private final static GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
			new GsonFactory())
			.setAudience(ImmutableList.of(CLIENT_ID))
			.build();

	private static final long DEFAULT_EXPIRATION_MS = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);

	private final Datastore datastore;

	public DatastoreAuthenticationManager(Datastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public AuthenticationResponse googleVerify(String namespace, String token) throws GeneralSecurityException {
		if (token == null) {
			log.warn("token is empty");
			throw new GeneralSecurityException();
		}

		GoogleIdToken idToken;
		try {
			idToken = verifier.verify(token);
		} catch (GeneralSecurityException | IOException e) {
			log.warn("failed to verify token: " + e);
			return null;
		}

		if (idToken == null) {
			log.warn("verifiction step returned null");
			throw new GeneralSecurityException();
		}

		String email = idToken.getPayload().getEmail();
		String subject = idToken.getPayload().getSubject();

		log.info("subject {} email {}", subject, email);

		Long userId = findOrCreateGoogleUser(idToken, email);

		String sessionId;
		try {
			sessionId = createSessionToken(userId);
		} catch (DatastoreException e) {
			log.warn("failed to store session for user {}: {}", idToken.getPayload().getSubject(), e);
			throw new GeneralSecurityException();
		}

		return AuthenticationResponse.builder()
				.id(userId)
				.session(sessionId)
				.build();
	}

	@Override
	public ListenableFuture<Void> verifySessionToken(String namespace, String session) {
		KeyQuery query = AuthQueries.fetchSession(namespace, session);

		return Futures.transform(
				datastore.executeAsync(query),
				(AsyncFunction<QueryResult, Void>)
				r ->
				{
					if (r.getEntity() == null) {
						log.warn("session {} not found", session);
						return Futures.immediateFailedFuture(new GeneralSecurityException());
					}

					return Futures.immediateFuture(null);
				});
	}

	private Long findOrCreateGoogleUser(GoogleIdToken idToken, String email)
			throws GeneralSecurityException {
		Long userId = null;

		try {
			userId = findGoogleUser(idToken.getPayload());
		} catch (DatastoreException e) {
			log.error("failed to find user {} : {}", idToken.getPayload().getEmail(), e);
			throw new GeneralSecurityException();
		} catch (UserNotFoundException e) {
			log.info("user {} doesn't exist", email);
		}

		if (userId == null) {
			try {
				userId = createGoogleUser(idToken.getPayload());
			} catch (DatastoreException e) {
				log.warn("failed to create user {}: {}", idToken.getPayload().getSubject(), e);
				throw new GeneralSecurityException();
			}
		}
		return userId;
	}

	private Long createGoogleUser(Payload userPayload) throws DatastoreException {
		User user = User.newBuilder()
				.setName(userPayload.getUnknownKeys().getOrDefault("name", "name to be set").toString())
				.setEmail(userPayload.getEmail())
				.setTimeZone(DEFAULT_TIME_ZONE)
				.build();

		Insert insert = UserQueries.insertUser(user)
				.value(Constants.GOOGLE_SUBJECT, userPayload.getSubject());

		return datastore.execute(insert).getInsertKey().getId();
	}

	private Long findGoogleUser(Payload userPayload) throws DatastoreException, UserNotFoundException {
		Query findUser = QueryBuilder.query().
				kindOf(Constants.USER_KIND)
				.filterBy(eq(Constants.GOOGLE_SUBJECT, userPayload.getSubject()));

		QueryResult result = datastore.execute(findUser);

		if (result.getEntity() == null) {
			throw new UserNotFoundException();
		}

		return result.getEntity().getKey().getId();
	}

	private String createSessionToken(long user) throws DatastoreException {
		String session = new BigInteger(130, random).toString(32);

		Insert insert = AuthQueries.insertSession(session, user, System.currentTimeMillis() + DEFAULT_EXPIRATION_MS);

		datastore.execute(insert);

		return session;
	}

}
