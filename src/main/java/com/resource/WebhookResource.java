package com.resource;

import com.service.MessageProcessorService;
import com.domain.Clinic;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/webhook")
public class WebhookResource {

    @Inject
    MessageProcessorService messageProcessorService;

    public record MessagePayload(String phone, String text) {
    }

    @POST
    @Path("/{instanceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            MessagePayload payload) {

        Clinic clinic = Clinic.find("instanceName", instanceName).firstResult();

        if (clinic == null || !apiKey.equals(clinic.webhookToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        messageProcessorService.processIncomingMessage(payload.phone(), payload.text(), clinic);

        return Response.ok().build();
    }

}
