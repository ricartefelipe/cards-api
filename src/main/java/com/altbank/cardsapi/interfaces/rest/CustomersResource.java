package com.altbank.cardsapi.interfaces.rest;

import com.altbank.cardsapi.application.dto.CustomerDetailResponse;
import com.altbank.cardsapi.application.dto.CustomerSummaryResponse;
import com.altbank.cardsapi.application.usecase.QueryCustomersUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@RolesAllowed("admin")
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomersResource {

    private final QueryCustomersUseCase queryCustomersUseCase;

    @Inject
    public CustomersResource(QueryCustomersUseCase queryCustomersUseCase) {
        this.queryCustomersUseCase = Objects.requireNonNull(queryCustomersUseCase, "queryCustomersUseCase");
    }

    @GET
    public List<CustomerSummaryResponse> list(@QueryParam("page") Integer page,
                                              @QueryParam("size") Integer size) {
        return queryCustomersUseCase.list(page, size);
    }

    @GET
    @Path("/{customerId}")
    public CustomerDetailResponse get(@PathParam("customerId") UUID customerId) {
        return queryCustomersUseCase.get(customerId);
    }
}
