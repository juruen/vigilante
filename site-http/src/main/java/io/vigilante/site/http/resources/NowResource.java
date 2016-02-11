package io.vigilante.site.http.resources;

import com.google.common.util.concurrent.Futures;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.CurrentTime;
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
@Path("/time")
public class NowResource {
    private static final Logger LOG = LoggerFactory.getLogger(NowResource.class);

    private final ResponseAdapter responseAdapter;

    public NowResource(ResponseAdapter responseAdapter) {
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    @Path("/now")
    public void getNow(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                       @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            Futures.immediateFuture(System.currentTimeMillis()),
            response,
            "get current time",
            c -> CurrentTime.builder().now(c).build());
    }

}
