package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.Teams;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.IncidentManager;
import io.vigilante.site.api.PaginationOptions;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.api.TeamManager;
import io.vigilante.site.http.model.Teams;
import io.vigilante.site.http.model.adapters.IncidentsAdapters;
import io.vigilante.site.http.model.adapters.SchedulesAdapters;
import io.vigilante.site.http.model.adapters.TeamsAdapters;
import io.vigilante.site.http.resources.util.PaginationUtil;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;


@Produces("application/json")
@Path("/team")
public class TeamResource {
    private final TeamManager teamManager;
    private final ScheduleManager scheduleManager;
    private final IncidentManager incidentManager;

    private final ResponseAdapter responseAdapter;

    public TeamResource(TeamManager teamManager,
                        ScheduleManager scheduleManager,
                        IncidentManager incidentManager,
                        ResponseAdapter responseAdapter) {
        this.teamManager = teamManager;
        this.scheduleManager = scheduleManager;
        this.incidentManager = incidentManager;
        this.responseAdapter = responseAdapter;
    }

    @POST
    @ManagedAsync
    public void addTeam(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                        Teams.Team team,
                        @Suspended final AsyncResponse response) {
        responseAdapter.processFutureAdd(
            authHeader,
            teamManager.addTeam(Constants.DEFAULT_NAMESPACE, TeamsAdapters.fromPojo(team)),
            response,
            Constants.CREATE_TEAM);
    }

    @PUT
    @Path("/{id}")
    @ManagedAsync
    public void modifyTeam(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           Teams.Team team,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            teamManager.updateTeam(Constants.DEFAULT_NAMESPACE, id, TeamsAdapters.fromPojo(team)),
            response,
            Constants.MODIFY_TEAM);
    }

    @GET
    @Path("/{id}")
    @ManagedAsync
    public void getTeam(@PathParam("id") long id,
                        @HeaderParam("X-Vigilante-Authorization") String authHeader,
                        @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            teamManager.getTeam(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_TEAM,
            team -> TeamsAdapters.fromProto(team));
    }

    @DELETE
    @Path("/{id}")
    @ManagedAsync
    public void deleteTeam(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            teamManager.deleteTeam(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.DELETE_TEAM);
    }

    @GET
    @Path("/{id}/schedules")
    @ManagedAsync
    public void getSchedulesForTeam(@PathParam("id") long id,
                                    @HeaderParam("X-Vigilante-Authorization") String authHeader,
                                    @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            scheduleManager.getSchedulesForTeam(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_SCHEDULE,
            schedules -> SchedulesAdapters.fromProto(schedules));
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
            incidentManager.getMergedIncidentsForTeam(Constants.DEFAULT_NAMESPACE, id, paging),
            response,
            Constants.GET_INCIDENTS,
            incidents -> IncidentsAdapters.fromProto(incidents));
    }
}