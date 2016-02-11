package io.vigilante.site.api;

import io.vigilante.IncidentProtos.Incident;
import io.vigilante.IncidentProtos.Incidents;

import com.google.common.util.concurrent.ListenableFuture;

public interface IncidentManager {
	ListenableFuture<Incident> getIncident(final String namespace, final String id);

	ListenableFuture<Incidents> getIncidents(final String namespace, final PaginationOptions pagination);

	ListenableFuture<Incidents> getOpenIncidents(final String namespace, final PaginationOptions pagination);

	ListenableFuture<Void> addIncident(final String namespace, final Incident Incident);

	ListenableFuture<Void> deleteIncident(final String namespace, final String id);

	ListenableFuture<Incidents> getIncidentsForService(final String namespace, final long id,
													   final PaginationOptions pagination);

	ListenableFuture<Incidents> getIncidentsForTeam(final String namespace, final long id,
													final PaginationOptions pagination);

	ListenableFuture<Incidents> getOpenIncidentsForService(final String namespace, final long id,
														   final PaginationOptions pagination);

	ListenableFuture<Incidents> getOpenIncidentsForTeam(final String namespace, final long id,
														final PaginationOptions pagination);

	ListenableFuture<Incident> getMergedIncident(final String namespace, final String id);

	ListenableFuture<Incidents> getMergedIncidents(final String namespace, final PaginationOptions pagination);

	ListenableFuture<Incidents> getMergedIncidentsForService(final String namespace, final long id,
															 final PaginationOptions pagination);

	ListenableFuture<Incidents> getMergedIncidentsForTeam(final String namespace, final long id,
														  final PaginationOptions pagination);

	ListenableFuture<Void> addIncidentToService(final String namespace, final long id, final Incident incident);

	ListenableFuture<Void> modifyIncident(String namespace, String id, Incident incident);

	ListenableFuture<Incident> getOpenIncident(final String namespace, final String id);
}
