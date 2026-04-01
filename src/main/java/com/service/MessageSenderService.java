package com.service;

import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.MessageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageSenderService {

    private static final Logger LOG = Logger.getLogger(MessageSenderService.class);

    @RestClient
    EvolutionApiClient evolutionApiClient;

    @ConfigProperty(name = "evolution-api.token")
    String apiKey;

    @ConfigProperty(name = "evolution-api.instance", defaultValue = "confirmaai-inst-1")
    String instanceName;

    public void sendWhatsAppMessage(String phone, String text) {
        try {
            MessageRequest request = new MessageRequest(phone, text, 1200);

            evolutionApiClient.sendMessage(apiKey, instanceName, request);
            LOG.infof("Mensagem disparada com sucesso para %s", phone);

        } catch (Exception e) {
            LOG.errorf("Falha de comunicação com a Evolution API ao enviar para %s: %s", phone, e.getMessage());
            // TODO: No futuro, podemos colocar as falhas numa fila para tentar de novo
        }
    }
}