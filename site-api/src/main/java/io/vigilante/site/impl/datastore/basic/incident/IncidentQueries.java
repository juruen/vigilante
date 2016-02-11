package io.vigilante.site.impl.datastore.basic.incident;

import static com.spotify.asyncdatastoreclient.QueryBuilder.desc;
import static com.spotify.asyncdatastoreclient.QueryBuilder.eq;
import static com.spotify.asyncdatastoreclient.QueryBuilder.gt;
import static com.spotify.asyncdatastoreclient.QueryBuilder.lt;
import io.vigilante.IncidentProtos.Incident;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.Random;

import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;

public class IncidentQueries {
	private static Random random = new Random();

	public static KeyQuery getIncident(String namespace, String id) {
		return QueryBuilder.query(Constants.INCIDENT_LOG_KIND, id);
	}

	public static Query getIncidents(PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_LOG_KIND);

		return setPagination(query, pagination);
	}

	public static Query getOpenIncidents(PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_KIND);

		return setPagination(query, pagination);
	}

	public static Query getIncidentsForService(long id, PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_LOG_KIND)
				.filterBy(eq(Constants.SERVICE, id));

		return setPagination(query, pagination);
	}

	public static Query getIncidentsForTeam(long id, PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_LOG_KIND)
				.filterBy(eq(Constants.TEAM, id));

		return setPagination(query, pagination);
	}

	public static Query getOpenIncidentsForService(long id, PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_KIND)
				.filterBy(eq(Constants.SERVICE_ID, id));

		return setPagination(query, pagination);
	}

	public static Query getOpenIncidentsForTeam(long id, PaginationOptions pagination) {
		Query query = QueryBuilder.query()
				.kindOf(Constants.INCIDENT_KIND)
				.filterBy(eq(Constants.TEAM_ID, id));

		return setPagination(query, pagination);
	}

	public static Insert addIncident(Incident incident) {
		return QueryBuilder.insert(Constants.INCIDENT_LOG_KIND, incident.getId())
				.value(Constants.DESCRIPTION, incident.getDescription())
				.value(Constants.START, incident.getStart())
				.value(Constants.SERVICE, incident.getService().getId())
				.value(Constants.TEAM, incident.getTeam().getId())
				.value(Constants.STATE, incident.getState().getNumber())
				.value(Constants.CREATION_TOKEN, creationToken(incident.getStart()));
	}

	private static long creationToken(long start) {
		return (start - (start % 1000)) + random.nextInt(1000);
	}

	public static Delete deleteIncident(long id) {
		return QueryBuilder.delete(Constants.INCIDENT_LOG_KIND, id);
	}

	public static Query getOpenIncident(String namespace, String id) {
		return QueryBuilder.query()
				.kindOf(Constants.INCIDENT_KIND)
				.filterBy(eq(Constants.INCIDENT_ID, id));
	}

	public static Query addPaginationFilter(Query query, final PaginationOptions pagination,
			long cursor) {
		if (pagination.getDirection() == PaginationOptions.Direction.DESCENDING) {
			query.filterBy(lt(Constants.CREATION_TOKEN, cursor));
		} else {
			query.filterBy(gt(Constants.CREATION_TOKEN, cursor));
		}

		return query;
	}

	private static Query setPagination(Query query, PaginationOptions pagination) {
		query.limit((int) Math.max(0, Math.min(Constants.MAX_ITEMS_PER_PAGE, pagination.getLimit())));

		return query.orderBy(desc(Constants.CREATION_TOKEN));
	}

}
