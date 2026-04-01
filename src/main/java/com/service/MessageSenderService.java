package com.service;

import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.MessageRequest;
import com.domain.Clinic;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageSenderService {

    private static final Logger LOG = Logger.getLogger(MessageSenderService.class);

    @RestClient
    EvolutionApiClient evolutionApiClient;

    public void sendWhatsAppMessage(String phone, String text, Clinic clinic) {
        if (clinic.instanceName == null || clinic.evolutionApiToken == null) {
            LOG.errorf("Clínica %s não possui instanceName ou token configurados.", clinic.name);
            return;
        }

        try {
            MessageRequest request = new MessageRequest(phone, text, 1200);

            evolutionApiClient.sendMessage(clinic.evolutionApiToken, clinic.instanceName, request);
            LOG.infof("Mensagem disparada com sucesso para %s via instância %s", phone, clinic.instanceName);

        } catch (Exception e) {
            LOG.errorf("Falha de comunicação com a Evolution API ao enviar para %s: %s", phone, e.getMessage());
        }
    }
}