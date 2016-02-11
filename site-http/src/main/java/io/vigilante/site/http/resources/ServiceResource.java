package io.vigilante.site.http.resources;

import io.vigilante.site.http.model.adapters.ServicesAdapters;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.api.ServiceManager;
import io.vigilante.site.http.model.Incidents;
import io.vigilante.site.http.model.Services;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import io.vigilante.site.http.model.adapters.ServicesAdapters;
import io.vigilante.site.http.resources.util.PaginationUtil;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;


@Produces("application/json")
@Path("/service")
public class ServiceResource {
    private final ServiceManager serviceManager;
    private final IncidentManager incidentManager;

    private final ResponseAdapter responseAdapter;

    public ServiceResource(ServiceManager serviceManager,
                           IncidentManager incidentManager,
                           ResponseAdapter responseAdapter)
    {
        this.serviceManager = serviceManager;
        this.incidentManager = incidentManager;
        this.responseAdapter = responseAdapter;
    }

    @POST
    @ManagedAsync
    public void addService(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                           Services.Service service,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureAdd(
            authHeader,
            serviceManager.addService(Constants.DEFAULT_NAMESPACE, ServicesAdapters.fromPojo(service)),
            response,
            Constants.CREATE_SERVICE);
    }

    @PUT
    @Path("/{id}")
    @ManagedAsync
    public void modifyService(@PathParam("id") long id,
                              @HeaderParam("X-Vigilante-Authorization") String authHeader,
                              Services.Service service,
                              @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            serviceManager.updateService(Constants.DEFAULT_NAMESPACE, id, ServicesAdapters.fromPojo(service)),
            response,
            Constants.MODIFY_SERVICE);
    }

    @GET
    @Path("/{id}")
    @ManagedAsync
    public void getService(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            serviceManager.getService(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_SERVICE,
            service -> ServicesAdapters.fromProto(service));
    }

    @DELETE
    @Path("/{id}")
    @ManagedAsync
    public void deleteService(@PathParam("id") long id,
                              @HeaderParam("X-Vigilante-Authorization") String authHeader,
                              @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            serviceManager.deleteService(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.DELETE_SERVICE);
    }

    @POST
    @Path("/{id}/incident")
    @ManagedAsync
    public void getService(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           Incidents.Incident incident,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            incidentManager.addIncidentToService(Constants.DEFAULT_NAMESPACE, id, IncidentsAdapters.fromPojo(incident)),
            response,
            Constants.CREATE_INCIDENT);
    }

    @GET
    @Path("/{id}/incidents")
    @ManagedAsync
    public void getIncidentsForTeam(@QueryParam("start") String start,
                                    @QueryParam("direction") String direction,
                                    @QueryParam("length") Long length,
                                    @PathParam("id") long id,
                                    @HeaderParam("X-Vigilante-Authorization") String authHeader,
                                    @Suspended final AsyncResponse response)
    {
        PaginationOptions paging;

        try {
            paging = PaginationUtil.toPaginationOptions(start, direction, length);
        } catch (IllegalArgumentException e) {
            response.resume(Response.status(Response.Status.BAD_REQUEST));
            return;
        }

        responseAdapter.processFutureGet(
            authHeader,
            incidentManager.getMergedIncidentsForService(Constants.DEFAULT_NAMESPACE, id, paging),
            response,
            Constants.GET_INCIDENTS,
            incidents -> IncidentsAdapters.fromProto(incidents));
    }
}