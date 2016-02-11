package io.vigilante.site.api.impl.datastore;

import io.vigilante.IncidentProtos.Incident;
import io.vigilante.IncidentProtos.Incidents;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.TeamProtos.Team;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.api.PaginationOptions.Direction;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.impl.datastore.basic.Constants;
import io.vigilante.site.impl.datastore.basic.incident.IncidentOperations;
import io.vigilante.site.impl.datastore.basic.service.ServiceOperations;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.protocol.HTTP;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.extra.ListenableFutureAdapter;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.futures.FuturesExtra;

@Slf4j
public class DatastoreIncidentManager implements IncidentManager {

	private final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

	private final IncidentOperations incidentOperations;
	private final TeamOperations teamOperations;
	private final ServiceOperations serviceOperations;

	public DatastoreIncidentManager(Datastore datastore) {
		this.incidentOperations = new IncidentOperations(datastore);
		this.teamOperations = new TeamOperations(datastore);
		this.serviceOperations = new ServiceOperations(datastore);
	}

	@Override
	public ListenableFuture<Incident> getIncident(String namespace, String id) {
		return incidentOperations.getIncident(namespace, id);
	}

	@Override
	public ListenableFuture<Incident> getOpenIncident(String namespace, String id) {
		return incidentOperations.getOpenIncident(namespace, id);
	}

	@Override
	public ListenableFuture<Incidents> getIncidents(String namespace, PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getIncidents(namespace, pagination), decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Incidents> getOpenIncidents(String namespace, PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getOpenIncidents(namespace, pagination),
				decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Void> addIncident(String namespace, Incident incident) {
		return incidentOperations.addIncident(namespace, incident);
	}

	@Override
	public ListenableFuture<Void> deleteIncident(String namespace, String id) {
		return incidentOperations.deleteIncident(namespace, Long.parseLong(id));
	}

	@Override
	public ListenableFuture<Incidents> getIncidentsForService(String namespace, long id,
			PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getIncidentsForService(namespace, id, pagination),
				decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Incidents> getIncidentsForTeam(String namespace, long id, PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getIncidentsForTeam(namespace, id, pagination),
				decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Incidents> getOpenIncidentsForService(String namespace, long id,
			PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getOpenIncidentsForService(namespace, id, pagination),
				decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Incidents> getOpenIncidentsForTeam(String namespace, long id, PaginationOptions pagination) {
		return Futures.transform(incidentOperations.getOpenIncidentsForTeam(namespace, id, pagination),
				decorateIncidents(namespace));
	}

	@Override
	public ListenableFuture<Incident> getMergedIncident(String namespace, String id) {
		return incidentOperations.getMergedIncident(namespace, id);
	}

	@Override
	public ListenableFuture<Incidents> getMergedIncidents(String namespace, PaginationOptions pagination) {
		ListenableFuture<Incidents> openIncidents = getOpenIncidents(namespace, pagination);
		ListenableFuture<Incidents> logIncidents = getIncidents(namespace, pagination);

		return FuturesExtra.syncTransform2(openIncidents, logIncidents,
				(o, l) -> buildMergedIncidents(o, l, pagination));
	}

	@Override
	public ListenableFuture<Incidents> getMergedIncidentsForService(String namespace, long id,
			PaginationOptions pagination) {
		ListenableFuture<Incidents> openIncidents = getOpenIncidentsForService(namespace, id, pagination);
		ListenableFuture<Incidents> logIncidents = getIncidentsForService(namespace, id, pagination);

		return FuturesExtra.syncTransform2(openIncidents, logIncidents,
				(o, l) -> buildMergedIncidents(o, l, pagination));
	}

	@Override
	public ListenableFuture<Incidents> getMergedIncidentsForTeam(String namespace, long id, PaginationOptions pagination) {
		ListenableFuture<Incidents> openIncidents = getOpenIncidentsForTeam(namespace, id, pagination);
		ListenableFuture<Incidents> logIncidents = getIncidentsForTeam(namespace, id, pagination);

		return FuturesExtra.syncTransform2(openIncidents, logIncidents,
				(o, l) -> buildMergedIncidents(o, l, pagination));
	}

	@Override
	public ListenableFuture<Void> addIncidentToService(String namespace, long id, Incident incident) {
		try {
			verifyAddIncident(incident);
		} catch (SiteExternalException e) {
			return Futures.immediateFailedFuture(e);
		}

		return FuturesExtra.asyncTransform(
				serviceOperations.getService(namespace, id),
				s -> pushIncidentChange(s.getKey(),
						incident.getKey(),
						incident.getDescription(),
						Incident.StateType.TRIGGERED.toString().toLowerCase()));
	}

	@Override
	public ListenableFuture<Void> modifyIncident(String namespace, String id, Incident incident) {
		try {
			verifyModifyIncident(incident);
		} catch (SiteExternalException e) {
			return Futures.immediateFailedFuture(e);
		}

		ListenableFuture<Incident> fetchIncident = getOpenIncident(namespace, id);
		ListenableFuture<Service> fetchService = FuturesExtra.asyncTransform(fetchIncident,
				i -> serviceOperations.getService(namespace, i.getService().getId()));

		return FuturesExtra.asyncTransform2(
				fetchIncident,
				fetchService,
				(i, s) -> pushIncidentChange(s.getKey(), i.getKey(), incident.getDescription(), incident.getState()
						.toString().toLowerCase()));
	}

	private void verifyAddIncident(Incident incident) throws SiteExternalException {
		if (!incident.hasKey()) {
			throw new SiteExternalException("Incident key is missing");
		}

		if (incident.hasState() && incident.getState() != Incident.StateType.TRIGGERED) {
			throw new SiteExternalException("Incident key is invalid");
		}

		if (!incident.hasDescription()) {
			throw new SiteExternalException("Description is missing");
		}
	}

	private void verifyModifyIncident(Incident incident) throws SiteExternalException {
		if (!incident.hasState() || incident.getState() == Incident.StateType.TRIGGERED) {
			throw new SiteExternalException(
					"Incident key is invalid, you can only resolve or acknowledge open incidents");
		}

		if (!incident.hasDescription()) {
			throw new SiteExternalException("Description is missing");
		}
	}

	private ListenableFuture<Void> pushIncidentChange(String serviceKey, String incidentKey, String description,
			String state) {
		JsonObject jsonParams = new JsonObject();

		jsonParams.addProperty("service", serviceKey);
		jsonParams.addProperty("incident", incidentKey);
		jsonParams.addProperty("description", description);
		jsonParams.addProperty("state", state);

		log.info("jsonParams {}", jsonParams.toString());

		BoundRequestBuilder preparePost = asyncHttpClient.preparePost(Constants.OPEN_INCIDENTS_URL)
				.setHeader(HTTP.CONTENT_TYPE, "application/json")
				.setBody(jsonParams.toString());

		return Futures.transform(
				ListenableFutureAdapter.asGuavaFuture(asyncHttpClient.executeRequest(preparePost.build())),
				(AsyncFunction<Response, Void>) (Response r) -> {
					if (r.getStatusCode() == 200) {
						return Futures.immediateFuture(null);
					} else {
						return Futures.immediateFailedFuture(new SiteExternalException("Failed to add incident"));
					}
				});
	}

	private Incidents buildMergedIncidents(Incidents open, Incidents log, PaginationOptions pagination) {
		List<Incident> incidents = new ArrayList<>();

		if (open != null) {
			incidents.addAll(open.getIncidentList());
		}

		if (log != null) {
			incidents.addAll(log.getIncidentList());
		}

		List<Incident> sortedIncidents = incidents.stream()
				.sorted((a, b) -> Long.compare(a.getStart(), b.getStart()))
				.limit(Math.min(pagination.getLimit(), Constants.MAX_ITEMS_PER_PAGE))
				.collect(Collectors.toList());

		if (pagination.getDirection() == Direction.DESCENDING) {
			Collections.reverse(sortedIncidents);
		}

		return Incidents.newBuilder().addAllIncident(sortedIncidents).build();
	}

	private AsyncFunction<Incidents, Incidents> decorateIncidents(String namespace) {
		return new AsyncFunction<Incidents, Incidents>() {

			@Override
			public ListenableFuture<Incidents> apply(Incidents incidents) throws Exception {
				Set<Long> services = new HashSet<>();
				Set<Long> teams = new HashSet<>();

				for (Incident i : incidents.getIncidentList()) {
					services.add(i.getService().getId());
					teams.add(i.getTeam().getId());
				}

				final List<ListenableFuture<Service>> serviceFutures = new ArrayList<>();
				final List<ListenableFuture<Team>> teamFutures = new ArrayList<>();

				services.forEach(id -> serviceFutures.add(serviceOperations.getService(namespace, id)));
				teams.forEach(id -> teamFutures.add(teamOperations.getTeam(namespace, id)));

				@SuppressWarnings("unchecked")
				ListenableFuture<List<Object>> servicesAndTeams = Futures.successfulAsList(
						Futures.successfulAsList(serviceFutures), Futures.successfulAsList(teamFutures));

				return Futures.transform(servicesAndTeams, doDecorateIncidents(incidents));
			}
		};
	}

	protected AsyncFunction<List<Object>, Incidents> doDecorateIncidents(Incidents incidents) {
		return new AsyncFunction<List<Object>, Incidents>() {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<Incidents> apply(List<Object> result) throws Exception {
				Map<Long, String> services = new HashMap<>();
				Map<Long, String> teams = new HashMap<>();

				if (result.get(0) != null) {
					for (Service service : (List<Service>) result.get(0)) {
						if (service == null) {
							continue;
						}
						services.put(service.getId(), service.getName());
					}
				}

				if (result.get(1) != null) {
					for (Team team : (List<Team>) result.get(1)) {
						if (team == null) {
							continue;
						}
						teams.put(team.getId(), team.getName());
					}
				}

				List<Incident> decoratedIncidents = new ArrayList<>();
				for (Incident incident : incidents.getIncidentList()) {
					String teamName = teams.getOrDefault(incident.getTeam().getId(), "Not Available");
					Team team = incident.getTeam().toBuilder().setName(teamName).build();

					String serviceName = services.getOrDefault(incident.getService().getId(), "Not Available");
					Service service = incident.getService().toBuilder().setName(serviceName).build();

					decoratedIncidents.add(incident.toBuilder().setTeam(team).setService(service).build());
				}

				return Futures.immediateFuture(Incidents.newBuilder().addAllIncident(decoratedIncidents).build());
			}
		};
	}

}
