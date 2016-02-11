package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.ScheduleManager;
import io.vigilante.site.http.model.adapters.SchedulesAdapters;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@Produces("application/json")
@Path("/schedules")
public class SchedulesResource {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulesResource.class);

    private final ScheduleManager scheduleManager;

    private final ResponseAdapter responseAdapter;

    public SchedulesResource(ScheduleManager scheduleManager, ResponseAdapter responseAdapter) {
        this.scheduleManager = scheduleManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    public void getSchedules(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                             @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            scheduleManager.getSchedules(Constants.DEFAULT_NAMESPACE),
            response,
            Constants.GET_SCHEDULE,
            schedules -> SchedulesAdapters.fromProto(schedules));
    }

}
