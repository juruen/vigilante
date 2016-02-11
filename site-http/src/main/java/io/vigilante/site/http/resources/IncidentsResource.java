package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import io.vigilante.site.http.resources.util.PaginationUtil;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

@Produces("application/json")
@Path("/incidents")
public class IncidentsResource {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentsResource.class);

    private final IncidentManager incidentManager;

    private final ResponseAdapter responseAdapter;

    public IncidentsResource(IncidentManager incidentManager, ResponseAdapter responseAdapter) {
        this.incidentManager = incidentManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    public void getIncidents(@QueryParam("start") String start,
                             @QueryParam("direction") String direction,
                             @QueryParam("length") Long length,
                             @HeaderParam("X-Vigilante-Authorization") String authHeader,
                             @Suspended final AsyncResponse response) {

        PaginationOptions paging;

        try {
             paging = PaginationUtil.toPaginationOptions(start, direction, length);
        } catch (IllegalArgumentException e) {
            response.resume(Response.status(Response.Status.BAD_REQUEST));
            return;
        }

        responseAdapter.processFutureGet(
            authHeader,
            incidentManager.getMergedIncidents(Constants.DEFAULT_NAMESPACE, paging),
            response,
            Constants.GET_INCIDENTS,
            incidents -> IncidentsAdapters.fromProto(incidents));
    }

}
