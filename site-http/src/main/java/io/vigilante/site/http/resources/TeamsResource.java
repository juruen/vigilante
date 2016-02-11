package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.adapters.TeamsAdapters;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.http.model.adapters.TeamsAdapters;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@Produces("application/json")
@Path("/teams")
public class TeamsResource {
    private static final Logger LOG = LoggerFactory.getLogger(TeamsResource.class);

    private final TeamManager teamManager;

    private final ResponseAdapter responseAdapter;

    public TeamsResource(TeamManager teamManager, ResponseAdapter responseAdapter) {
        this.teamManager = teamManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    public void getTeams(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                         @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            teamManager.getTeams(
                Constants.DEFAULT_NAMESPACE),
                response,
                Constants.GET_TEAMS,
            teams -> TeamsAdapters.fromProto(teams));
    }

    @GET
    @Path("/user/{id}")
    @ManagedAsync
    public void getTeam(@PathParam("id") long id,
                        @HeaderParam("X-Vigilante-Authorization") String authHeader,
                        @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            teamManager.getTeamsForUser(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_TEAMS,
            team -> TeamsAdapters.fromProto(team));
    }
}
