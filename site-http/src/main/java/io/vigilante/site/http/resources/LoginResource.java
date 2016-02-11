package io.vigilante.site.http.resources;

import com.google.common.util.concurrent.Futures;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.http.model.LoginRequest;
import io.vigilante.site.http.model.LoginResponse;
import io.vigilante.site.http.Constants;
import io.vigilante.site.http.ResponseAdapter;
import io.vigilante.site.api.AuthenticationManager;
import io.vigilante.site.api.AuthenticationResponse;
import io.vigilante.site.http.model.LoginRequest;
import io.vigilante.site.http.model.LoginResponse;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.security.GeneralSecurityException;

@Produces("application/json")
@Path("/login")
public class LoginResource {
    private static final Logger LOG = LoggerFactory.getLogger(LoginResource.class);

    private final AuthenticationManager authenticationManager;

    private final ResponseAdapter responseAdapter;

    public LoginResource(AuthenticationManager authenticationManager, ResponseAdapter responseAdapter) {
        this.authenticationManager = authenticationManager;
        this.responseAdapter = responseAdapter;
    }

    @GET
    @ManagedAsync
    @Path("/google")
    public void authGooogle(LoginRequest login,
                            @Suspended final AsyncResponse response) throws GeneralSecurityException {
        AuthenticationResponse authResposne;

        try {
            authResposne = authenticationManager.googleVerify(Constants.DEFAULT_NAMESPACE, login.getToken());
        } catch (GeneralSecurityException e) {
            response.resume(Response.status(Response.Status.UNAUTHORIZED));
            return;
        }

        responseAdapter.processFutureGetNoAuth(
            Futures.immediateFuture(LoginResponse.builder()
                    .id(authResposne.getId())
                    .token(authResposne.getSession())
                    .build()),
            response,
            "google auth",
            r -> r);
    }

}
