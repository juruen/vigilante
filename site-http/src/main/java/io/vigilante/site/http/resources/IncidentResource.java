package io.vigilante.site.http.resources;

import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.http.model.Incidents;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;


@Produces("application/json")
@Path("/incident")
public class IncidentResource {
    private final IncidentManager incidentManager;

    private final ResponseAdapter responseAdapter;

    public IncidentResource(IncidentManager incidentManager, ResponseAdapter responseAdapter) {
        this.incidentManager = incidentManager;
        this.responseAdapter = responseAdapter;
    }


    @PUT
    @Path("/{id}")
    @ManagedAsync
    public void modifyIncident(@PathParam("id") String id,
                               @HeaderParam("X-Vigilante-Authorization") String authHeader,
                               Incidents.Incident incident,
                               @Suspended final AsyncResponse response) {
        /*
        responseAdapter.processFutureModify(
            authHeader,
            incidentManager.modifyIncident(Constants.DEFAULT_NAMESPACE, id, IncidentsAdapters.fromPojo(incident)),
            response,
            Constants.MODIFY_INCIDENT);
            */
    }

}