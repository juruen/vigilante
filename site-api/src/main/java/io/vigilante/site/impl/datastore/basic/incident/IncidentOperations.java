package io.vigilante.site.impl.datastore.basic.incident;

import io.vigilante.IncidentProtos.Incident;
import io.vigilante.IncidentProtos.Incidents;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryResult;

public class IncidentOperations {

	private final Datastore datastore;

	public IncidentOperations(final Datastore datastore) {
		this.datastore = datastore;
	}

	public ListenableFuture<Incident> getIncident(final @NonNull String namespace, final String id) {
		return Futures.transform(datastore.executeAsync(IncidentQueries.getIncident(namespace, id)), buildIncident());
	}

	public ListenableFuture<Incident> getOpenIncident(final @NonNull String namespace, final String id) {
		return Futures.transform(datastore.executeAsync(IncidentQueries.getOpenIncident(namespace, id)),
				buildOpenIncident());
	}

	public ListenableFuture<Void> addIncident(final @NonNull String namespace, final Incident incident) {
		if (!incident.hasDescription()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Description is missing"));
		}

		if (!incident.hasStart()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Start is missing"));
		}

		if (!incident.hasService()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Service is missing"));
		}

		if (!incident.hasTeam()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Team is missing"));
		}

		return Futures.transform(datastore.executeAsync(IncidentQueries.addIncident(incident)),
				AsyncUtil.emptyResponse());
	}

	public ListenableFuture<Incidents> getIncidents(final @NonNull String namespace,
			final @NonNull PaginationOptions pagination) {
		return getIncidentsWithPagination(namespace, pagination, IncidentQueries.getIncidents(pagination));
	}

	public ListenableFuture<Incidents> getOpenIncidents(final @NonNull String namespace,
			final @NonNull PaginationOptions pagination) {
		return getOpenIncidentsWithPagination(namespace, pagination, IncidentQueries.getOpenIncidents(pagination));
	}

	public ListenableFuture<Incidents> getIncidentsForService(final @NonNull String namespace, final long id,
			final @NonNull PaginationOptions pagination) {
		return getIncidentsWithPagination(namespace, pagination, IncidentQueries.getIncidentsForService(id, pagination));
	}

	public ListenableFuture<Incidents> getIncidentsForTeam(final @NonNull String namespace, final long id,
			final @NonNull PaginationOptions pagination) {

		return getIncidentsWithPagination(namespace, pagination, IncidentQueries.getIncidentsForTeam(id, pagination));
	}

	public ListenableFuture<Incidents> getOpenIncidentsForService(final @NonNull String namespace, final long id,
			final @NonNull PaginationOptions pagination) {

		return getOpenIncidentsWithPagination(namespace, pagination,
				IncidentQueries.getOpenIncidentsForService(id, pagination));
	}

	public ListenableFuture<Incidents> getOpenIncidentsForTeam(final @NonNull String namespace, final long id,
			final @NonNull PaginationOptions pagination) {

		return getOpenIncidentsWithPagination(namespace, pagination,
				IncidentQueries.getOpenIncidentsForTeam(id, pagination));
	}

	public ListenableFuture<Incident> getMergedIncident(String namespace, String id) {
		ListenableFuture<QueryResult> openQuery = datastore
				.executeAsync(IncidentQueries.getOpenIncident(namespace, id));
		ListenableFuture<QueryResult> logQuery = datastore.executeAsync(IncidentQueries.getIncident(namespace, id));

		ListenableFuture<List<QueryResult>> futures = Futures.successfulAsList(ImmutableList.of(openQuery, logQuery));

		return Futures.transform(futures, (AsyncFunction<List<QueryResult>, Incident>) r -> buildMergedIncident(r));
	}

	public ListenableFuture<Void> deleteIncident(final @NonNull String namespace, final long id) {
		return Futures.transform(datastore.executeAsync(IncidentQueries.deleteIncident(id)), AsyncUtil.emptyResponse());
	}

	private ListenableFuture<Incident> buildMergedIncident(List<QueryResult> r) {
		QueryResult openResult = r.get(0);

		if (openResult.getEntity() != null) {
			return Futures.immediateFuture(IncidentUtil.buildBasicOpenIncident(openResult.getEntity()));
		}

		QueryResult logResult = r.get(1);

		if (logResult.getEntity() != null) {
			return Futures.immediateFuture(IncidentUtil.buildBasicIncident(openResult.getEntity()));
		}

		return Futures.immediateFailedFuture(new SiteExternalException("incident doesn't exist"));

	}

	private ListenableFuture<Incidents> getIncidentsWithPagination(final String namespace,
			final PaginationOptions pagination, Query query) {
		if (pagination.getFromId() == null) {
			if (pagination.getDirection() == PaginationOptions.Direction.ASCENDING) {
				return Futures.immediateFailedFuture(new SiteExternalException("Invalid pagination options"));
			} else {
				return Futures.transform(datastore.executeAsync(query), buildIncidents());
			}
		}

		ListenableFuture<QueryResult> incidents = Futures.transform(
				datastore.executeAsync(IncidentQueries.getIncident(namespace, pagination.getFromId())),
				fetcIncidents(query, pagination));

		return Futures.transform(incidents, buildIncidents());
	}

	private ListenableFuture<Incidents> getOpenIncidentsWithPagination(final String namespace,
			final PaginationOptions pagination, Query query) {
		if (pagination.getFromId() == null) {
			if (pagination.getDirection() == PaginationOptions.Direction.ASCENDING) {
				return Futures.immediateFailedFuture(new SiteExternalException("Invalid pagination options"));
			} else {
				return Futures.transform(datastore.executeAsync(query), buildOpenIncidents());
			}
		}

		ListenableFuture<QueryResult> incidents = Futures.transform(
				datastore.executeAsync(IncidentQueries.getIncident(namespace, pagination.getFromId())),
				fetcIncidents(query, pagination));

		return Futures.transform(incidents, buildOpenIncidents());
	}

	private AsyncFunction<QueryResult, QueryResult> fetcIncidents(final Query query, final PaginationOptions pagination) {
		return new AsyncFunction<QueryResult, QueryResult>() {

			@Override
			public ListenableFuture<QueryResult> apply(QueryResult cursor) throws Exception {
				if (cursor.getEntity() == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Invalid pagination options"));
				}

				Query paginationQuery = IncidentQueries.addPaginationFilter(query, pagination, cursor.getEntity()
						.getInteger(Constants.CREATION_TOKEN));

				return datastore.executeAsync(paginationQuery);
			}
		};
	}

	private AsyncFunction<QueryResult, Incidents> buildIncidents() {
		return new AsyncFunction<QueryResult, Incidents>() {

			@Override
			public ListenableFuture<Incidents> apply(QueryResult result) throws Exception {
				List<Incident> incidents = new ArrayList<>();

				for (Entity e : result.getAll()) {
					incidents.add(IncidentUtil.buildBasicIncident(e));
				}

				return Futures.immediateFuture(Incidents.newBuilder().addAllIncident(incidents).build());
			}

		};
	}

	private AsyncFunction<QueryResult, Incidents> buildOpenIncidents() {
		return new AsyncFunction<QueryResult, Incidents>() {

			@Override
			public ListenableFuture<Incidents> apply(QueryResult result) throws Exception {
				List<Incident> incidents = new ArrayList<>();

				for (Entity e : result.getAll()) {
					incidents.add(IncidentUtil.buildBasicOpenIncident(e));
				}

				return Futures.immediateFuture(Incidents.newBuilder().addAllIncident(incidents).build());
			}

		};
	}

	private AsyncFunction<QueryResult, Incident> buildOpenIncident() {
		return new AsyncFunction<QueryResult, Incident>() {

			@Override
			public ListenableFuture<Incident> apply(QueryResult result) throws Exception {
				if (result.getEntity() == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Open incident doesn't exist"));
				}

				return Futures.immediateFuture(IncidentUtil.buildBasicOpenIncident(result.getEntity()));
			}

		};
	}

	private AsyncFunction<QueryResult, Incident> buildIncident() {
		return new AsyncFunction<QueryResult, Incident>() {

			@Override
			public ListenableFuture<Incident> apply(QueryResult result) throws Exception {
				Entity e = result.getEntity();

				if (e == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Incident doesn't exist"));
				}

				return Futures.immediateFuture(IncidentUtil.buildBasicIncident(e));
			}

		};
	}

}