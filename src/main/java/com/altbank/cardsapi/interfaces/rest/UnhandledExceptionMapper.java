package com.altbank.cardsapi.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jboss.logging.Logger;

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(UnhandledExceptionMapper.class);

    private final Clock clock;

    @Inject
    public UnhandledExceptionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        String path = uriInfo == null ? null : uriInfo.getPath();
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            ErrorResponse body = new ErrorResponse(
                    "NOT_FOUND",
                    "Resource not found",
                    404,
                    path,
                    LocalDateTime.now(clock),
                    null
            );
            return Response.status(404)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }
        LOG.error("Unhandled exception", exception);
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected error",
                500,
                path,
                LocalDateTime.now(clock),
                null
        );
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
