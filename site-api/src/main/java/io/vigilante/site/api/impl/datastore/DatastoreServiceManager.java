package io.vigilante.site.api.impl.datastore;

import io.vigilante.ScheduleProtos.Schedule;
import io.vigilante.ServiceProtos.Service;
import io.vigilante.ServiceProtos.Services;
import io.vigilante.TeamProtos.Team;
import io.vigilante.site.api.ServiceManager;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.basic.schedule.ScheduleOperations;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;
import io.vigilante.site.impl.datastore.basic.service.ServiceOperations;
import io.vigilante.site.impl.datastore.basic.team.TeamOperations;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;

public class DatastoreServiceManager implements ServiceManager {

	private final ServiceOperations serviceOperations;
	private final TeamOperations teamOperations;
	private final ScheduleOperations scheduleOperations;

	public DatastoreServiceManager(Datastore datastore) {
		this.serviceOperations = new ServiceOperations(datastore);
		this.teamOperations = new TeamOperations(datastore);
		this.scheduleOperations = new ScheduleOperations(datastore);
	}

	@Override
	public ListenableFuture<Service> getService(String namespace, long id) {
		ListenableFuture<Service> teamDecorated = Futures.transform(serviceOperations.getService(namespace, id),
				decorateTeam(namespace));
		return Futures.transform(teamDecorated, decorateSchedule(namespace));
	}

	@Override
	public ListenableFuture<Services> getServices(String namespace) {
		return serviceOperations.getServices(namespace);
	}

	@Override
	public ListenableFuture<Long> addService(String namespace, Service service) {
		try {
			verifyService(service);
		} catch (Exception e) {
			return Futures.immediateFailedFuture(e);
		}

		ListenableFuture<Void> teamExists = teamOperations.teamExists(namespace, service.getTeam().getId());
		ListenableFuture<Void> scheduleExists = scheduleOperations.scheduleExists(namespace, service.getSchedule()
				.getId());

		ListenableFuture<Long> addService = serviceOperations.addService(namespace, service);

		return AsyncUtil.conditionalAdd(addService, teamExists, scheduleExists);
	}

	@Override
	public ListenableFuture<Void> updateService(String namespace, long id, Service service) {
		try {
			verifyService(service);
		} catch (Exception e) {
			return Futures.immediateFailedFuture(e);
		}

		ListenableFuture<Void> teamExists = teamOperations.teamExists(namespace, service.getTeam().getId());
		ListenableFuture<Void> scheduleExists = scheduleOperations.scheduleExists(namespace, service.getSchedule()
				.getId());
		ListenableFuture<Void> updateService = serviceOperations.updateService(namespace, id, service);

		return AsyncUtil.conditionalUpdate(updateService, teamExists, scheduleExists);
	}

	private void verifyService(Service service) throws SiteExternalException {
		if (!service.hasTeam() || !service.getTeam().hasId()) {
			throw new SiteExternalException("Team is missing");
		}

		if (!service.hasSchedule() || !service.getSchedule().hasId()) {
			throw new SiteExternalException("Schedule is missing");
		}
	}

	@Override
	public ListenableFuture<Void> deleteService(String namespace, long id) {
		return serviceOperations.deleteService(namespace, id);
	}

	@Override
	public ListenableFuture<Services> getServicesForTeam(String namespace, long id) {
		return serviceOperations.getServicesForTeam(namespace, id);
	}

	@Override
	public ListenableFuture<Services> getServicesForSchedule(String namespace, long id) {
		return serviceOperations.getServicesForSchedule(namespace, id);
	}

	private AsyncFunction<Service, Service> decorateTeam(final String namespace) {
		return new AsyncFunction<Service, Service>() {

			@Override
			public ListenableFuture<Service> apply(Service service) throws Exception {

				ListenableFuture<Team> teamWithFallback = Futures.withFallback(
						teamOperations.getTeam(namespace, service.getTeam().getId()), AsyncUtil.teamFallback());

				return Futures.transform(teamWithFallback, decorateTeam(service));
			}
		};
	}

	private AsyncFunction<Service, Service> decorateSchedule(final String namespace) {
		return new AsyncFunction<Service, Service>() {

			@Override
			public ListenableFuture<Service> apply(Service service) throws Exception {

				ListenableFuture<Schedule> scheduleWithFallback = Futures.withFallback(
						scheduleOperations.getSchedule(namespace, service.getSchedule().getId()),
						AsyncUtil.scheduleFallback());

				return Futures.transform(scheduleWithFallback, decorateSchedule(service));
			}
		};
	}

	private AsyncFunction<Team, Service> decorateTeam(final Service service) {
		return new AsyncFunction<Team, Service>() {

			@Override
			public ListenableFuture<Service> apply(Team team) throws Exception {
				return Futures.immediateFuture(service.toBuilder().setTeam(team.toBuilder().clearUsers()).build());
			}
		};
	}

	private AsyncFunction<Schedule, Service> decorateSchedule(final Service service) {
		return new AsyncFunction<Schedule, Service>() {

			@Override
			public ListenableFuture<Service> apply(Schedule schedule) throws Exception {
				return Futures.immediateFuture(service.toBuilder()
						.setSchedule(schedule.toBuilder().clearScheduleLevels().clearTeam().build()).build());
			}
		};
	}
}
