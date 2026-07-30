package com.altbank.cardsapi.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.PhysicalCardResponse;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardRequest;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardResponse;
import com.altbank.cardsapi.application.dto.ValidatePhysicalCardResponse;
import com.altbank.cardsapi.application.usecase.QueryPhysicalCardsUseCase;
import com.altbank.cardsapi.application.usecase.ReissuePhysicalCardUseCase;
import com.altbank.cardsapi.application.usecase.ValidatePhysicalCardUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@RolesAllowed("user")
@Path("/physical-cards")
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalCardsResource {

    private final ValidatePhysicalCardUseCase validatePhysicalCardUseCase;
    private final ReissuePhysicalCardUseCase reissuePhysicalCardUseCase;
    private final QueryPhysicalCardsUseCase queryPhysicalCardsUseCase;

    @Inject
    public PhysicalCardsResource(ValidatePhysicalCardUseCase validatePhysicalCardUseCase,
                                 ReissuePhysicalCardUseCase reissuePhysicalCardUseCase,
                                 QueryPhysicalCardsUseCase queryPhysicalCardsUseCase) {
        this.validatePhysicalCardUseCase = Objects.requireNonNull(validatePhysicalCardUseCase, "validatePhysicalCardUseCase");
        this.reissuePhysicalCardUseCase = Objects.requireNonNull(reissuePhysicalCardUseCase, "reissuePhysicalCardUseCase");
        this.queryPhysicalCardsUseCase = Objects.requireNonNull(queryPhysicalCardsUseCase, "queryPhysicalCardsUseCase");
    }

    @GET
    public List<PhysicalCardResponse> list(@QueryParam("accountId") UUID accountId) {
        return queryPhysicalCardsUseCase.list(accountId);
    }

    @GET
    @Path("/{cardId}")
    public PhysicalCardResponse get(@PathParam("cardId") UUID cardId) {
        return queryPhysicalCardsUseCase.get(cardId);
    }

    @POST
    @Path("/{cardId}/validate")
    public ValidatePhysicalCardResponse validate(@PathParam("cardId") UUID cardId) {
        return validatePhysicalCardUseCase.validate(cardId);
    }

    @POST
    @Path("/{cardId}/reissue")
    @Consumes(MediaType.APPLICATION_JSON)
    public ReissuePhysicalCardResponse reissue(@PathParam("cardId") UUID cardId, @Valid ReissuePhysicalCardRequest request) {
        return reissuePhysicalCardUseCase.reissue(cardId, request);
    }
}
