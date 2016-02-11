package io.vigilante.site.http.resources;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

@Path("/")
public class OptionsResource {
    private static final Logger LOG = LoggerFactory.getLogger(OptionsResource.class);

    @OPTIONS
    @ManagedAsync
    @Path("/{subResources:.*}")
    public void getOptions(@Suspended final AsyncResponse response) {
        response.resume(Response.ok().build());
    }

}
