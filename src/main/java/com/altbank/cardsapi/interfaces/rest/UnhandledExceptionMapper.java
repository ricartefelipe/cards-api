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

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

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
