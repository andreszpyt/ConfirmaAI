package com.resource;

import com.dto.WhatsAppWebhook;
import com.domain.Clinic;
import com.service.CsvProcessorService;
import com.service.MessageProcessorService;
import com.service.MessageSenderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;

@Path("/api/webhook")
public class WebhookResource {

    @Inject
    MessageProcessorService messageProcessorService;

    @Inject
    CsvProcessorService csvProcessorService;

    @Inject
    MessageSenderService messageSenderService;

    @POST
    @Path("/{instanceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            WhatsAppWebhook payload) {

        Clinic clinic = Clinic.find("instanceName", instanceName).firstResult();

        if (clinic == null || clinic.webhookToken == null || !clinic.webhookToken.equals(apiKey)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (payload.data() == null) {
            return Response.ok().build();
        }

        String phone = payload.data().remoteJid();
        String messageType = payload.data().messageType();

        if ("documentMessage".equals(messageType) && payload.data().message() != null && payload.data().message().base64() != null) {
            try {
                byte[] decodedCsv = Base64.getDecoder().decode(payload.data().message().base64());
                int scheduled = csvProcessorService.processAgendaCsv(decodedCsv, clinic);

                String cleanPhone = phone.replace("@s.whatsapp.net", "");
                messageSenderService.sendWhatsAppMessage(cleanPhone, "✅ Planilha recebida com sucesso! Processamos e agendamos " + scheduled + " confirmações.");
            } catch (Exception e) {
                System.err.println("Erro ao processar documento: " + e.getMessage());
            }
        }
        else if (payload.data().message() != null && payload.data().message().conversation() != null) {
            messageProcessorService.processIncomingMessage(phone, payload.data().message().conversation(), clinic);
        }

        return Response.ok().build();
    }
}