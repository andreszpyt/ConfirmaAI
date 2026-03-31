package com.service;

import com.ai.IntentExtractionService;
import com.ai.IntentExtractionService.IntentExtractionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.domain.Patient;
import com.domain.Appointment;
import com.domain.Appointment.AppointmentStatus;

@ApplicationScoped
public class MessageProcessorService {

    @Inject
    IntentExtractionService intentExtractionService;

    @Transactional
    public void processIncomingMessage(String phone, String text) {
        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        switch (result.action()) {
            case CONFIRM:
                System.out.println("Confirmando consulta do paciente " + result.patientName());
                break;
            case CANCEL:
                String cleanPhone = phone.replace("@s.whatsapp.net", "");
                Patient patient = Patient.find("whatsappPhone", cleanPhone).firstResult();
                if (patient != null) {
                    Appointment appointment = Appointment.find("patient = ?1 and status = 'PENDING'", patient).firstResult();
                    if (appointment != null) {
                        appointment.status = AppointmentStatus.CANCELED;
                        appointment.persist();
                    }
                }
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
