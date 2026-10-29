package com.altbank.cardsapi.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.AccountDetailResponse;
import com.altbank.cardsapi.application.dto.AccountSummaryResponse;
import com.altbank.cardsapi.application.dto.CancelAccountResponse;
import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.dto.IssueVirtualCardResponse;
import com.altbank.cardsapi.application.usecase.CancelAccountUseCase;
import com.altbank.cardsapi.application.usecase.CreateAccountUseCase;
import com.altbank.cardsapi.application.usecase.IssueVirtualCardUseCase;
import com.altbank.cardsapi.application.usecase.QueryAccountsUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@RolesAllowed("admin")
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountsResource {

    private final CreateAccountUseCase createAccountUseCase;
    private final IssueVirtualCardUseCase issueVirtualCardUseCase;
    private final CancelAccountUseCase cancelAccountUseCase;
    private final QueryAccountsUseCase queryAccountsUseCase;

    @Inject
    public AccountsResource(CreateAccountUseCase createAccountUseCase,
                            IssueVirtualCardUseCase issueVirtualCardUseCase,
                            CancelAccountUseCase cancelAccountUseCase,
                            QueryAccountsUseCase queryAccountsUseCase) {
        this.createAccountUseCase = Objects.requireNonNull(createAccountUseCase, "createAccountUseCase");
        this.issueVirtualCardUseCase = Objects.requireNonNull(issueVirtualCardUseCase, "issueVirtualCardUseCase");
        this.cancelAccountUseCase = Objects.requireNonNull(cancelAccountUseCase, "cancelAccountUseCase");
        this.queryAccountsUseCase = Objects.requireNonNull(queryAccountsUseCase, "queryAccountsUseCase");
    }

    @GET
    public List<AccountSummaryResponse> list(@QueryParam("page") Integer page,
                                             @QueryParam("size") Integer size) {
        return queryAccountsUseCase.list(page, size);
    }

    @GET
    @Path("/{accountId}")
    public AccountDetailResponse get(@PathParam("accountId") UUID accountId) {
        return queryAccountsUseCase.get(accountId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
