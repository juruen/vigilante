package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.adapters.ServicesAdapters;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.ServiceManager;
import io.vigilante.site.http.model.adapters.ServicesAdapters;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@Produces("application/json")
@Path("/services")
public class ServicesResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServicesResource.class);

    private final ServiceManager serviceManager;

    private final ResponseAdapter responseAdapter;

    public ServicesResource(ServiceManager serviceManager, ResponseAdapter responseAdapter) {
        this.serviceManager = serviceManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    public void getServices(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                            @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            serviceManager.getServices(
                Constants.DEFAULT_NAMESPACE),
                response,
                Constants.GET_SERVICES,
            services -> ServicesAdapters.fromProto(services));
    }

    @GET
    @Path("/team/{id}")
    @ManagedAsync
    public void getServicesForTeam(@PathParam("id") long id,
                                   @HeaderParam("X-Vigilante-Authorization") String authHeader,
                                   @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            serviceManager.getServicesForTeam(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_SERVICES,
            services -> ServicesAdapters.fromProto(services));
    }

    @GET
    @Path("/schedule/{id}")
    @ManagedAsync
    public void getServicesForSchedule(@PathParam("id") long id,
                                       @HeaderParam("X-Vigilante-Authorization") String authHeader,
                                       @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            serviceManager.getServicesForSchedule(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_SERVICES,
            services -> ServicesAdapters.fromProto(services));
    }
}
