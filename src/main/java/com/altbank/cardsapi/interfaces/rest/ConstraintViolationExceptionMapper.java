package com.altbank.cardsapi.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private final Clock clock;

    @Inject

    public ConstraintViolationExceptionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String path = uriInfo == null ? null : uriInfo.getPath();
        List<ErrorResponse.FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .toList();

        ErrorResponse body = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                400,
                path,
                LocalDateTime.now(clock),
                violations
        );

        return Response.status(400)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private ErrorResponse.FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString();
        return new ErrorResponse.FieldViolation(field, violation.getMessage());
    }
}
