package io.vigilante.site.http;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.vigilante.site.api.AuthenticationManager;
import io.vigilante.site.api.exceptions.SiteExternalException;
import io.vigilante.site.http.model.AddResponse;
import io.vigilante.site.http.model.ErrorResponse;
import io.vigilante.site.http.model.ModifyResponse;
import io.vigilante.site.http.model.adapters.Adapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;

public class ResponseAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseAdapter.class);

    private final AuthenticationManager authenticationManager;

    private final Executor executor;

    public ResponseAdapter(AuthenticationManager authenticationManager, Executor executor) {
        this.authenticationManager = authenticationManager;
        this.executor = executor;
    }

    public <A, B> void processFutureGet(String authHeader,
                                        ListenableFuture<A> future,
                                        AsyncResponse response,
                                        String msg,
                                        Adapter<A,B> adapter)
    {
        Futures.addCallback(Futures.transform(authorize(authHeader), (Void i) -> future), new FutureCallback<A>() {
            @Override
            public void onSuccess(A a) {
                response.resume(adapter.adapt(a));
            }

            @Override
            public void onFailure(Throwable throwable) {
                handleFailure(throwable, msg, response);
            }
        },
        executor);
    }

    public <A, B> void processFutureGetNoAuth(ListenableFuture<A> future,
                                              AsyncResponse response,
                                              String msg,
                                              Adapter<A,B> adapter) {
        Futures.addCallback(future, new FutureCallback<A>() {

                @Override
                public void onSuccess(A a) {
                    response.resume(adapter.adapt(a));
                }

                @Override
                public void onFailure(Throwable throwable) {
                    handleFailure(throwable, msg, response);
                }
            },
            executor);
    }


    public void processFutureAdd(String authHeader, ListenableFuture<Long> future, AsyncResponse response, String msg) {
        Futures.addCallback(Futures.transform(authorize(authHeader), (Void i) -> future), new FutureCallback<Long>()
        {

                @Override
            public void onSuccess(Long id) {
                response.resume(AddResponse.builder()
                    .id(id.toString())
                    .message(String.format("%s successful", msg))
                    .build());
            }

            @Override
            public void onFailure(Throwable throwable) {
                handleFailure(throwable, msg, response);
            }
        });
    }

    public void processFutureModify(String authHeader,
                                    ListenableFuture<Void> future,
                                    AsyncResponse response, String msg)
    {
        Futures.addCallback(Futures.transform(authorize(authHeader), (Void i) -> future), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                response.resume(ModifyResponse.builder()
                    .message(String.format("%s successful", msg))
                    .build());
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(t, msg, response);

            }
        });
    }

    private void handleFailure(Throwable throwable, String msg, AsyncResponse response) {
        String message;
        int status = Response.Status.BAD_REQUEST.getStatusCode();

        if (throwable instanceof SiteExternalException) {
            message = throwable.getMessage();
        } else if (throwable instanceof GeneralSecurityException) {
            message = "unauthorized";
            status = Response.Status.UNAUTHORIZED.getStatusCode();
        } else {
            LOG.error("internal error when {} {}", msg, throwable);
            message = "internal error";
        }

        ErrorResponse errorResponse = ErrorResponse.builder().message(message).build();

        response.resume(Response.status(status).entity(errorResponse).build());
    }

    private ListenableFuture<Void> authorize(String authHeader) {
        if (authHeader == null) {
            return Futures.immediateFailedCheckedFuture(new GeneralSecurityException("Failed authentication"));
        }

        String token = authHeader.replaceFirst("Bearer ", "");

        return authenticationManager.verifySessionToken(Constants.DEFAULT_NAMESPACE, token);
    }
}
