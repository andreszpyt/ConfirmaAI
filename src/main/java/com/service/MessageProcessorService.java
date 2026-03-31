package com.service;

import com.ai.IntentExtractionService;
import com.ai.IntentExtractionService.IntentExtractionResult;
import com.domain.Clinic;
import com.domain.Patient;
import com.domain.Appointment;
import com.domain.Appointment.AppointmentStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageProcessorService {

    private static final String CLINIC_PHONE = "5585999999999";

    @Inject
    IntentExtractionService intentExtractionService;

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    @Inject
    MessageSenderService messageSenderService;

    @Transactional
    public void processIncomingMessage(String phone, String text, Clinic clinic) {
        String cleanPhone = phone.replace("@s.whatsapp.net", "");

        Patient patient = Patient.find("whatsappPhone", cleanPhone).firstResult();
        if (patient == null) {
            System.out.println("Paciente não encontrado para o número: " + cleanPhone);
            return;
        }

        Appointment appointment = Appointment.find("patient.whatsappPhone = ?1 and clinic = ?2 and status = 'PENDING'", cleanPhone, clinic).firstResult();
        if (appointment == null) {
            System.out.println("Nenhuma consulta PENDENTE encontrada para o paciente: " + patient.name);
            return;
        }

        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        String replyToPatient = "";

        switch (result.action()) {
            case CONFIRM:
                appointment.status = AppointmentStatus.CONFIRMED;
                replyToPatient = "Ótimo! Sua consulta está confirmada. Nos vemos em breve.";
                messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "Notificação: Consulta do paciente " + patient.name + " foi confirmada.");
                break;
            case CANCEL:
                appointment.status = AppointmentStatus.CANCELED;
                replyToPatient = "Tudo bem, sua consulta foi cancelada. Obrigado por avisar!";
                messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "Notificação: Consulta do paciente " + patient.name + " foi cancelada.");
                break;
            case RESCHEDULE:
                appointment.status = AppointmentStatus.NEEDS_HUMAN;
                replyToPatient = "Certo, um de nossos atendentes vai falar com você em instantes para encontrarmos um novo horário.";
                messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "ALERTA: O paciente " + patient.name + " solicitou reagendamento.");
                break;
            default:
                System.out.println("Intenção não reconhecida com segurança. Mantendo status atual.");
                return;
        }

        appointment.persist();
        messageSenderService.sendWhatsAppMessage(cleanPhone, replyToPatient);

        if (appointment.quartzJobId != null) {
            try {
                appointmentSchedulerService.cancelSchedule(appointment.quartzJobId);
                System.out.println("[QUARTZ] Agendamento cancelado para consulta " + appointment.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}