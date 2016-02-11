package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.Schedules;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.http.model.Schedules;
import io.vigilante.site.http.model.adapters.SchedulesAdapters;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;


@Produces("application/json")
@Path("/schedule")
public class ScheduleResource {
    private final ScheduleManager scheduleManager;

    private final ResponseAdapter responseAdapter;

    public ScheduleResource(ScheduleManager scheduleManager, ResponseAdapter responseAdapter) {
        this.scheduleManager = scheduleManager;
        this.responseAdapter = responseAdapter;
    }

    @POST
    @ManagedAsync
    public void addSchedule(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                            Schedules.Schedule schedule, @Suspended final AsyncResponse response) {
        responseAdapter.processFutureAdd(
            authHeader,
            scheduleManager.addSchedule(Constants.DEFAULT_NAMESPACE, SchedulesAdapters.fromPojo(schedule)),
            response,
            Constants.CREATE_SCHEDULE);
    }

    @PUT
    @Path("/{id}")
    @ManagedAsync
    public void modifySchedule(@PathParam("id") long id,
                               @HeaderParam("X-Vigilante-Authorization") String authHeader,
                               Schedules.Schedule schedule,
                               @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            scheduleManager.updateSchedule(Constants.DEFAULT_NAMESPACE, id, SchedulesAdapters.fromPojo(schedule)),
            response,
            Constants.MODIFY_SCHEDULE);
    }

    @GET
    @Path("/{id}")
    @ManagedAsync
    public void getSchedule(@PathParam("id") long id,
                            @HeaderParam("X-Vigilante-Authorization") String authHeader,
                            @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            scheduleManager.getSchedule(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_SCHEDULE,
            schedule -> SchedulesAdapters.fromProto(schedule));
    }

    @DELETE
    @Path("/{id}")
    @ManagedAsync
    public void deleteSchedule(@PathParam("id") long id,
                               @HeaderParam("X-Vigilante-Authorization") String authHeader,
                               @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            scheduleManager.deleteSchedule(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.DELETE_SCHEDULE);
    }
}