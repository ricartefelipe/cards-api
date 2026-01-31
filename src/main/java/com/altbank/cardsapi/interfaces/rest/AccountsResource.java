package com.altbank.cardsapi.interfaces.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CancelAccountResponse;
import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.dto.IssueVirtualCardResponse;
import com.altbank.cardsapi.application.usecase.CancelAccountUseCase;
import com.altbank.cardsapi.application.usecase.CreateAccountUseCase;
import com.altbank.cardsapi.application.usecase.IssueVirtualCardUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AccountsResource {

    private final CreateAccountUseCase createAccountUseCase;
    private final IssueVirtualCardUseCase issueVirtualCardUseCase;
    private final CancelAccountUseCase cancelAccountUseCase;

    @Inject

    public AccountsResource(CreateAccountUseCase createAccountUseCase,
                            IssueVirtualCardUseCase issueVirtualCardUseCase,
                            CancelAccountUseCase cancelAccountUseCase) {
        this.createAccountUseCase = Objects.requireNonNull(createAccountUseCase, "createAccountUseCase");
        this.issueVirtualCardUseCase = Objects.requireNonNull(issueVirtualCardUseCase, "issueVirtualCardUseCase");
        this.cancelAccountUseCase = Objects.requireNonNull(cancelAccountUseCase, "cancelAccountUseCase");
    }

    @POST
    public Response create(@Valid CreateAccountRequest request) {
        CreateAccountResponse response = createAccountUseCase.create(request);
        return Response.created(URI.create("/accounts/" + response.accountId()))
                .entity(response)
                .build();
    }

    @POST
    @Path("/{accountId}/virtual-cards")
    public IssueVirtualCardResponse issueVirtual(@PathParam("accountId") UUID accountId) {
        return issueVirtualCardUseCase.issue(accountId);
    }

    @POST
    @Path("/{accountId}/cancel")
    public CancelAccountResponse cancel(@PathParam("accountId") UUID accountId) {
        return cancelAccountUseCase.cancel(accountId);
    }
}
