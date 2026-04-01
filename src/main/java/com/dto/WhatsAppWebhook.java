package com.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhook(String instance, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("remoteJid") String remoteJid,
            String pushName,
            Message message,
            String messageType) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Message(
                String conversation,
                String base64) {}
    }
}