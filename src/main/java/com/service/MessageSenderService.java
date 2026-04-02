package com.service;

import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.MessageRequest;
import com.domain.Clinic;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Fallback;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class MessageSenderService {

    private static final Logger LOG = Logger.getLogger(MessageSenderService.class);

    @RestClient
    EvolutionApiClient evolutionApiClient;

    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "handleDefinitiveFailure")
    public void sendWhatsAppMessage(String phone, String text, Clinic clinic) {

        if (clinic.instanceName == null || clinic.evolutionApiToken == null) {
            LOG.errorf("Clínica %s não possui instanceName ou token configurados.", clinic.name);
            return;
        }

        MessageRequest request = new MessageRequest(phone, text, 1200);

        evolutionApiClient.sendMessage(clinic.evolutionApiToken, clinic.instanceName, request);
        LOG.infof("Mensagem disparada com sucesso para %s via instância %s", phone, clinic.instanceName);
    }

    public void handleDefinitiveFailure(String phone, String text, Clinic clinic, Exception e) {
        LOG.errorf("FALHA DEFINITIVA ao enviar mensagem para %s após retentativas. Motivo: %s", phone, e.getMessage());

    }
}