package editor.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import io.vertx.core.http.HttpServerRequest;

@RequestScoped
public class RequestContextService {

    @Context
    HttpHeaders headers;

    @Inject
    io.vertx.ext.web.RoutingContext routingContext;

    public String tenant() {
        return header("X-Tenant-Id");
    }

    public String ip() {
        try {
            String forwarded = header("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
            return routingContext.request().remoteAddress().host();
        } catch (Throwable t) { return null; }
    }

    public String userAgent() {
        return header("User-Agent");
    }

    private String header(String name) {
        try { var v = headers.getHeaderString(name); return v == null || v.isBlank() ? null : v; }
        catch (Throwable t) { return null; }
    }
}

