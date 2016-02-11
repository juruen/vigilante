package io.vigilante.site.util;

import java.util.Arrays;
import java.util.List;

import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;

public class TestHelper {

	private static final List<String> KINDS = Arrays.asList("schedule", "time_range", "team", "user", "notification",
			"service");

	public static void removeAll(Datastore datastore) throws Exception {
		for (String kind : KINDS) {
			removeAll(datastore, kind);
		}
	}

	private static void removeAll(final Datastore datastore, final String kind) throws Exception {
		final Query queryAll = QueryBuilder.query().kindOf(kind).keysOnly();
		for (final Entity entity : datastore.execute(queryAll)) {
			datastore.execute(QueryBuilder.delete(entity.getKey()));
		}
	}
}
