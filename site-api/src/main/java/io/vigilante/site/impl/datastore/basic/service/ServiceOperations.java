package io.vigilante.site.impl.datastore.basic.service;

import io.vigilante.ServiceProtos.Service;
import io.vigilante.ServiceProtos.Services;
import io.vigilante.site.api.ServiceManager;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.api.impl.datastore.util.AsyncUtil;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.MutationResult;
import com.spotify.asyncdatastoreclient.MutationStatement;
import com.spotify.asyncdatastoreclient.QueryResult;

public class ServiceOperations implements ServiceManager {

	private final Datastore datastore;

	public ServiceOperations(final Datastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public ListenableFuture<Service> getService(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ServiceQueries.getService(id)), buildService());
	}

	@Override
	public ListenableFuture<Services> getServices(String namespace) {
		return Futures.transform(datastore.executeAsync(ServiceQueries.getServices()), buildServices());
	}

	@Override
	public ListenableFuture<Long> addService(String namespace, Service service) {
		ListenableFuture<MutationResult> insert = Futures.transform(verifyService(namespace, service),
				mutateService(ServiceQueries.insertService(service)));

		return Futures.transform(insert, AsyncUtil.fetchId());
	}

	@Override
	public ListenableFuture<Void> updateService(String namespace, long id, Service service) {
		ListenableFuture<MutationResult> update = Futures.transform(verifyService(namespace, service),
				mutateService(ServiceQueries.updateService(id, service)));

		return Futures.transform(update, AsyncUtil.emptyResponse());
	}

	@Override
	public ListenableFuture<Void> deleteService(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ServiceQueries.deleteService(id)), AsyncUtil.emptyResponse());
	}

	@Override
	public ListenableFuture<Services> getServicesForTeam(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ServiceQueries.getServicesForTeam(id)), buildServices());
	}

	@Override
	public ListenableFuture<Services> getServicesForSchedule(String namespace, long id) {
		return Futures.transform(datastore.executeAsync(ServiceQueries.getServicesForSchedule(id)), buildServices());
	}

	private ListenableFuture<Service> verifyService(final @NonNull String namespace, final @NonNull Service service) {
		if (!service.hasName()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Name is missing"));
		}

		if (!service.hasTeam()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Team is missing"));
		}

		if (!service.hasSchedule()) {
			return Futures.immediateFailedFuture(new SiteExternalException("Schedule is missing"));
		}

		return Futures.immediateFuture(service);
	}

	private AsyncFunction<QueryResult, Services> buildServices() {
		return new AsyncFunction<QueryResult, Services>() {

			@Override
			public ListenableFuture<Services> apply(QueryResult result) throws Exception {
				List<Service> services = new ArrayList<>();

				if (result.getEntity() != null) {
					for (Entity e : result.getAll()) {
						services.add(ServiceUtil.buildBasicService(e));
					}
				}

				Services.Builder builder = Services.newBuilder();

				if (!services.isEmpty()) {
					builder.addAllServices(services);
				}

				return Futures.immediateFuture(builder.build());
			}
		};
	}

	private AsyncFunction<Service, MutationResult> mutateService(final MutationStatement mutation) {
		return new AsyncFunction<Service, MutationResult>() {

			@Override
			public ListenableFuture<MutationResult> apply(Service Service) throws Exception {
				return datastore.executeAsync(mutation);
			}
		};
	}

	private AsyncFunction<QueryResult, Service> buildService() {
		return new AsyncFunction<QueryResult, Service>() {

			@Override
			public ListenableFuture<Service> apply(QueryResult result) throws Exception {
				Entity e = result.getEntity();
				
				if (e == null) {
					return Futures.immediateFailedFuture(new SiteExternalException("Service doesn't exist"));
				}

				return Futures.immediateFuture(ServiceUtil.buildBasicService(e));
			}

		};
	}
}
