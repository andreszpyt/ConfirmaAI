package com.resource;

import com.dto.WhatsAppWebhook;
import com.domain.Clinic;
import com.logging.StructuredEventLog;
import com.service.CsvProcessorService;
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
import org.jboss.logging.Logger;

@Path("/api/webhook")
public class WebhookResource {

    private static final Logger LOG = Logger.getLogger(WebhookResource.class);

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
            LOG.warnf("flow=webhook_receive phase=reject outcome=unauthorized instanceName=%s", instanceName);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Long clinicId = clinic.id;

        if (payload.data() == null) {
            StructuredEventLog.webhookReceive(LOG, "start", clinicId, "n/a", "n/a", "empty_payload", "ignored");
            StructuredEventLog.webhookReceive(LOG, "end", clinicId, "n/a", "n/a", "empty_payload", "ignored");
            return Response.ok().build();
        }

        String phone = payload.data().remoteJid();
        String cleanPhoneDigits = phone.replace("@s.whatsapp.net", "").replaceAll("\\D", "");
        String messageType = payload.data().messageType();

        if ("documentMessage".equals(messageType) && payload.data().message() != null && payload.data().message().base64() != null) {
            String cleanPhone = phone.replace("@s.whatsapp.net", "");
            StructuredEventLog.webhookReceive(LOG, "start", clinicId, cleanPhoneDigits, "n/a", "document_csv", "accepted");
            try {
                byte[] decodedCsv = Base64.getDecoder().decode(payload.data().message().base64());
                int scheduled = csvProcessorService.processAgendaCsv(decodedCsv, clinic);
                messageSenderService.sendWhatsAppMessage(cleanPhone, "✅ Planilha recebida com sucesso! Processamos e agendamos " + scheduled + " confirmações.", clinic);
                StructuredEventLog.webhookReceive(
                        LOG, "end", clinicId, cleanPhoneDigits, "n/a", "document_csv", "success scheduledCount=" + scheduled);
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "flow=webhook_receive phase=error clinicId=%d patientPhone=%s appointmentId=n/a handler=document_csv",
                        clinicId,
                        StructuredEventLog.maskPhoneDigitsForLog(cleanPhoneDigits));
                messageSenderService.sendWhatsAppMessage(cleanPhone, "❌ Ops! Tivemos um problema ao ler sua planilha. Verifique se o arquivo está no formato CSV correto e tente novamente.", clinic);
                StructuredEventLog.webhookReceive(LOG, "end", clinicId, cleanPhoneDigits, "n/a", "document_csv", "csv_processing_failed");
            }
        } else {
            StructuredEventLog.webhookReceive(LOG, "start", clinicId, cleanPhoneDigits, "n/a", messageType != null ? messageType : "unknown_type", "no_handler");
            StructuredEventLog.webhookReceive(
                    LOG, "end", clinicId, cleanPhoneDigits, "n/a", messageType != null ? messageType : "unknown_type", "ack_only");
        }

        return Response.ok().build();
    }
}