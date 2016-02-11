package io.vigilante.site.http.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class ResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException
    {
        containerResponseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        containerResponseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        containerResponseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Vigilante-Authorization");
        containerResponseContext.getHeaders().add( "Access-Control-Max-Age", "86400");
    }
}
