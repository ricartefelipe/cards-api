package com.altbank.cardsapi.interfaces.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardRequest;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardResponse;
import com.altbank.cardsapi.application.usecase.GetVirtualCardCvvUseCase;
import com.altbank.cardsapi.application.usecase.ReissueVirtualCardUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@Path("/virtual-cards")
@Produces(MediaType.APPLICATION_JSON)
public class VirtualCardsResource {

    private final GetVirtualCardCvvUseCase getVirtualCardCvvUseCase;
    private final ReissueVirtualCardUseCase reissueVirtualCardUseCase;

    @Inject

    public VirtualCardsResource(GetVirtualCardCvvUseCase getVirtualCardCvvUseCase,
                                ReissueVirtualCardUseCase reissueVirtualCardUseCase) {
        this.getVirtualCardCvvUseCase = Objects.requireNonNull(getVirtualCardCvvUseCase, "getVirtualCardCvvUseCase");
        this.reissueVirtualCardUseCase = Objects.requireNonNull(reissueVirtualCardUseCase, "reissueVirtualCardUseCase");
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
