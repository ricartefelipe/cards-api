package com.altbank.cardsapi.interfaces.rest;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.exception.AppException;
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
public class AppExceptionMapper implements ExceptionMapper<AppException> {

    private final Clock clock;

    @Inject

    public AppExceptionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AppException exception) {
        String path = uriInfo == null ? null : uriInfo.getPath();
        ErrorResponse body = new ErrorResponse(
                exception.errorCode().name(),
                exception.getMessage(),
                exception.httpStatus(),
                path,
                LocalDateTime.now(clock),
                null
        );
        return Response.status(exception.httpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
