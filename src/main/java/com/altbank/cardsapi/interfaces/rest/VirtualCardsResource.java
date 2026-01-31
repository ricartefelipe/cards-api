package com.altbank.cardsapi.interfaces.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.usecase.GetVirtualCardCvvUseCase;
import jakarta.ws.rs.GET;
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

    @Inject

    public VirtualCardsResource(GetVirtualCardCvvUseCase getVirtualCardCvvUseCase) {
        this.getVirtualCardCvvUseCase = Objects.requireNonNull(getVirtualCardCvvUseCase, "getVirtualCardCvvUseCase");
    }

    @GET
    @Path("/{cardId}/cvv")
    public CvvResponse getCvv(@PathParam("cardId") UUID cardId) {
        return getVirtualCardCvvUseCase.get(cardId);
    }
}
