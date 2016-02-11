package io.vigilante.site.http.resources;

import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.UserManager;
import io.vigilante.site.http.model.Users;
import io.vigilante.site.http.model.adapters.UsersAdapters;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;


@Produces("application/json")
@Path("/user")
public class UserResource {
    private final UserManager userManager;

    private final ResponseAdapter responseAdapter;

    public UserResource(UserManager userManager, ResponseAdapter responseAdapter) {
        this.userManager = userManager;
        this.responseAdapter = responseAdapter;
    }

    @POST
    @ManagedAsync
    public void addUser(@HeaderParam("X-Vigilante-Authorization") String authHeader,
                        Users.User user,
                        @Suspended final AsyncResponse response) {
        responseAdapter.processFutureAdd(
            authHeader,
            userManager.addUser(Constants.DEFAULT_NAMESPACE, UsersAdapters.fromPojo(user)),
            response,
            Constants.CREATE_USER);
    }

    @PUT
    @Path("/{id}")
    @ManagedAsync
    public void modifyUser(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           Users.User user,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            userManager.updateUser(Constants.DEFAULT_NAMESPACE, id, UsersAdapters.fromPojo(user)),
            response,
            Constants.MODIFY_USER);
    }

    @GET
    @Path("/{id}")
    @ManagedAsync
    public void getUser(@PathParam("id") long id,
                        @HeaderParam("X-Vigilante-Authorization") String authHeader,
                        @Suspended final AsyncResponse response) {
        responseAdapter.processFutureGet(
            authHeader,
            userManager.getUser(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.GET_USER,
            user -> UsersAdapters.fromProto(user));
    }

    @DELETE
    @Path("/{id}")
    @ManagedAsync
    public void deleteUser(@PathParam("id") long id,
                           @HeaderParam("X-Vigilante-Authorization") String authHeader,
                           @Suspended final AsyncResponse response) {
        responseAdapter.processFutureModify(
            authHeader,
            userManager.deleteUser(Constants.DEFAULT_NAMESPACE, id),
            response,
            Constants.DELETE_USER);
    }
}