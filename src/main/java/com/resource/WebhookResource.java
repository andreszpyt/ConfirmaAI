package com.resource;

import com.service.MessageProcessorService;
import com.dto.WhatsAppWebhook;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/webhook/whatsapp")
public class WebhookResource {

    @Inject
    MessageProcessorService messageProcessorService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(WhatsAppWebhook webhook) {
        String phone = webhook.data().remoteJid();
        String text = webhook.data().message().conversation();

        messageProcessorService.processIncomingMessage(phone, text);

        return Response.ok().build();
    }

}
