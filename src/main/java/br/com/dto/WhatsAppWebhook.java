package br.com.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhook(String instance, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String remoteJid, String pushName, Message message) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Message(String conversation) {}

    }

}
