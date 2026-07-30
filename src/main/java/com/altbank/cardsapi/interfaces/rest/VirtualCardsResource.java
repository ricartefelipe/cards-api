package com.altbank.cardsapi.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardRequest;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardResponse;
import com.altbank.cardsapi.application.dto.VirtualCardResponse;
import com.altbank.cardsapi.application.usecase.GetVirtualCardCvvUseCase;
import com.altbank.cardsapi.application.usecase.QueryVirtualCardsUseCase;
import com.altbank.cardsapi.application.usecase.ReissueVirtualCardUseCase;
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
@Path("/virtual-cards")
@Produces(MediaType.APPLICATION_JSON)
public class VirtualCardsResource {

    private final GetVirtualCardCvvUseCase getVirtualCardCvvUseCase;
    private final ReissueVirtualCardUseCase reissueVirtualCardUseCase;
    private final QueryVirtualCardsUseCase queryVirtualCardsUseCase;

    @Inject
    public VirtualCardsResource(GetVirtualCardCvvUseCase getVirtualCardCvvUseCase,
                                ReissueVirtualCardUseCase reissueVirtualCardUseCase,
                                QueryVirtualCardsUseCase queryVirtualCardsUseCase) {
        this.getVirtualCardCvvUseCase = Objects.requireNonNull(getVirtualCardCvvUseCase, "getVirtualCardCvvUseCase");
        this.reissueVirtualCardUseCase = Objects.requireNonNull(reissueVirtualCardUseCase, "reissueVirtualCardUseCase");
        this.queryVirtualCardsUseCase = Objects.requireNonNull(queryVirtualCardsUseCase, "queryVirtualCardsUseCase");
    }

    @GET
    public List<VirtualCardResponse> list(@QueryParam("accountId") UUID accountId) {
        return queryVirtualCardsUseCase.list(accountId);
    }

    @GET
    @Path("/{cardId}")
    public VirtualCardResponse get(@PathParam("cardId") UUID cardId) {
        return queryVirtualCardsUseCase.get(cardId);
    }

    @GET
    @Path("/{cardId}/cvv")
    public CvvResponse getCvv(@PathParam("cardId") UUID cardId) {
        return getVirtualCardCvvUseCase.get(cardId);
    }

    @POST
    @Path("/{cardId}/reissue")
    @Consumes(MediaType.APPLICATION_JSON)
    public ReissueVirtualCardResponse reissue(@PathParam("cardId") UUID cardId, @Valid ReissueVirtualCardRequest request) {
        return reissueVirtualCardUseCase.reissue(cardId, request);
    }
}
