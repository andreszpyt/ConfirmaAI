package com.service;

import com.ai.IntentExtractionService;
import com.ai.IntentExtractionService.IntentExtractionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageProcessorService {

    @Inject
    IntentExtractionService intentExtractionService;

    public void processIncomingMessage(String phone, String text) {
        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        switch (result.action()) {
            case CONFIRM:
                System.out.println("Confirmando consulta do paciente " + result.patientName());
                break;
            case CANCEL:
                System.out.println("Cancelando consulta do paciente " + result.patientName());
                break;
            case RESCHEDULE:
                System.out.println("Remarcando consulta do paciente " + result.patientName() + " de " + result.originalTime() + " para " + result.newTime());
                break;
            case ADD:
                System.out.println("Adicionando nova consulta para o paciente " + result.patientName() + " em " + result.newTime());
                break;
            case UNKNOWN:
            default:
                System.out.println("Intenção desconhecida. Mensagem: " + text);
                break;
        }
    }

}
