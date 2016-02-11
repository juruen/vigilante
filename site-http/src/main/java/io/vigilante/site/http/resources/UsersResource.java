package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.http.model.adapters.UsersAdapters;
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
@Path("/users")
public class UsersResource {
    private static final Logger LOG = LoggerFactory.getLogger(UsersResource.class);

    private final UserManager userManager;

    private final ResponseAdapter responseAdapter;

    public UsersResource(UserManager userManager, ResponseAdapter responseAdapter) {
        this.userManager = userManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    public void getUsers(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                         @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            userManager.getUsers(
                Constants.DEFAULT_NAMESPACE),
                response,
                Constants.GET_USERS,
            users -> UsersAdapters.fromProto(users));
    }

}
