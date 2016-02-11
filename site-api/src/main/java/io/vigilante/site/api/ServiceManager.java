package io.vigilante.site.api;

import io.vigilante.ServiceProtos.Service;
import io.vigilante.ServiceProtos.Services;

import com.google.common.util.concurrent.ListenableFuture;

public interface ServiceManager {

	ListenableFuture<Service> getService(final String namespace, final long id);

	ListenableFuture<Services> getServices(final String namespace);

	ListenableFuture<Long> addService(final String namespace, final Service service);

	ListenableFuture<Void> updateService(final String namespace, final long id, final Service team);

	ListenableFuture<Void> deleteService(final String namespace, final long id);

	ListenableFuture<Services> getServicesForTeam(final String namespace, final long id);

	ListenableFuture<Services> getServicesForSchedule(final String namespace, final long id);

}
