package com.altbank.cardsapi.interfaces.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CarrierDeliveryWebhookRequest;
import com.altbank.cardsapi.application.dto.ProcessorCvvRotationWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.usecase.HandleCarrierDeliveryWebhookUseCase;
import com.altbank.cardsapi.application.usecase.HandleProcessorCvvRotationWebhookUseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;

@ApplicationScoped

@Path("/webhooks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WebhooksResource {

    private final HandleCarrierDeliveryWebhookUseCase handleCarrierDeliveryWebhookUseCase;
    private final HandleProcessorCvvRotationWebhookUseCase handleProcessorCvvRotationWebhookUseCase;

    @Inject

    public WebhooksResource(HandleCarrierDeliveryWebhookUseCase handleCarrierDeliveryWebhookUseCase,
                            HandleProcessorCvvRotationWebhookUseCase handleProcessorCvvRotationWebhookUseCase) {
        this.handleCarrierDeliveryWebhookUseCase = Objects.requireNonNull(handleCarrierDeliveryWebhookUseCase, "handleCarrierDeliveryWebhookUseCase");
        this.handleProcessorCvvRotationWebhookUseCase = Objects.requireNonNull(handleProcessorCvvRotationWebhookUseCase, "handleProcessorCvvRotationWebhookUseCase");
    }

    @POST
    @Path("/carrier/delivery")
    public WebhookAckResponse carrierDelivery(@Valid CarrierDeliveryWebhookRequest request) {
        return handleCarrierDeliveryWebhookUseCase.handle(request);
    }

    @POST
    @Path("/processor/cvv-rotation")
    public WebhookAckResponse processorCvvRotation(@Valid ProcessorCvvRotationWebhookRequest request) {
        return handleProcessorCvvRotationWebhookUseCase.handle(request);
    }
}
