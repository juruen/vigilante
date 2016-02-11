package io.vigilante.site.impl.datastore.basic.service;

import static com.spotify.asyncdatastoreclient.QueryBuilder.asc;
import static com.spotify.asyncdatastoreclient.QueryBuilder.eq;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.UUID;

import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.Update;

public class ServiceQueries {

	public static Query getServices() {
		return QueryBuilder.query()
				.kindOf(Constants.SERVICE_KIND)
				.orderBy(asc(Constants.NAME));
	}

	public static Insert insertService(final Service service) {
		return QueryBuilder.insert(Constants.SERVICE_KIND)
				.value(Constants.NAME, service.getName())
				.value(Constants.API_KEY, UUID.randomUUID().toString().replaceAll("-", ""))
				.value(Constants.TEAM, service.getTeam().getId())
				.value(Constants.SCHEDULE, service.getSchedule().getId());
	}

	public static Update updateService(final long id, final Service service) {
		return QueryBuilder.update(Key.builder(Constants.SERVICE_KIND, id).build())
				.value(Constants.NAME, service.getName())
				.value(Constants.TEAM, service.getTeam().getId())
				.value(Constants.API_KEY, service.getKey())
				.value(Constants.SCHEDULE, service.getSchedule().getId());
	}

	public static Delete deleteService(final long id) {
		return QueryBuilder.delete(Constants.SERVICE_KIND, id);
	}

	public static KeyQuery getService(final long id) {
		return QueryBuilder.query(Constants.SERVICE_KIND, id);
	}

	public static Query getServicesForTeam(final long id) {
		return QueryBuilder.query()
				.kindOf(Constants.SERVICE_KIND)
				.filterBy(eq(Constants.TEAM, id));
	}

	public static Query getServicesForSchedule(final long id) {
		return QueryBuilder.query()
				.kindOf(Constants.SERVICE_KIND)
				.filterBy(eq(Constants.SCHEDULE, id));
	}

}
