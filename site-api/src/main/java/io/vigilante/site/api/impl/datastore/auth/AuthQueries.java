package io.vigilante.site.api.impl.datastore.auth;

import io.vigilante.site.impl.datastore.basic.Constants;

import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.QueryBuilder;

public class AuthQueries {

	public static Insert insertSession(String session, long user, long expires) {
		return QueryBuilder.insert(Constants.SESSION_KIND, session)
				.value(Constants.USER, user)
				.value(Constants.EXPIRES, expires);
	}

	public static KeyQuery fetchSession(String namespace, String session) {
		return QueryBuilder.query(Constants.SESSION_KIND, session);
	}

}
