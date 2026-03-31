package com.service;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DefaultBean
public class ConsoleMessageSender implements MessageSenderService {

    @Override
    public void sendWhatsAppMessage(String phone, String message) {
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ Sending WhatsApp message to: " + phone + " │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ " + message + " │");
        System.out.println("└─────────────────────────────────────┘");
    }
}
