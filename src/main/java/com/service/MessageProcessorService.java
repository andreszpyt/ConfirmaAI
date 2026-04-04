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

import java.util.List;
import java.util.Arrays;

@ApplicationScoped
public class MessageProcessorService {

    @Inject
    IntentExtractionService intentExtractionService;

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    @Inject
    MessageSenderService messageSenderService;

    @Transactional
    public void processIncomingMessage(String phone, String text, Clinic clinic) {
        String cleanPhone = phone.replace("@s.whatsapp.net", "").replaceAll("\\D", "");

        List<String> phoneVariations = generatePhoneVariations(cleanPhone);
        Patient patient = Patient.find("whatsappPhone IN ?1", phoneVariations).firstResult();

        if (patient == null) {
            System.out.println("Paciente não encontrado para o número (ou variações): " + cleanPhone);
            return;
        }

        Appointment appointment = Appointment.find(
                "patient = ?1 and clinic = ?2 and status = 'PENDING' ORDER BY scheduledAt ASC",
                patient, clinic).firstResult();

        if (appointment == null) {
            System.out.println("Nenhuma consulta PENDENTE encontrada para o paciente: " + patient.name);
            return;
        }

        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        String replyToPatient = "";

        switch (result.action()) {
            case CONFIRM:
                appointment.status = AppointmentStatus.CONFIRMED;
                replyToPatient = clinic.msgTemplateConfirm;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "✅ Notificação: Consulta do paciente " + patient.name + " foi CONFIRMADA.", clinic);
                }
                break;
            case CANCEL:
                appointment.status = AppointmentStatus.CANCELED;
                replyToPatient = clinic.msgTemplateCancel;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "❌ Notificação: Consulta do paciente " + patient.name + " foi CANCELADA.", clinic);
                }
                break;
            case RESCHEDULE:
                appointment.status = AppointmentStatus.NEEDS_HUMAN;
                replyToPatient = clinic.msgTemplateReschedule;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "⚠️ ALERTA: O paciente " + patient.name + " solicitou REAGENDAMENTO.", clinic);
                }
                break;
            default:
                System.out.println("Intenção não reconhecida com segurança. Mantendo status atual.");
                return;
        }

        appointment.persist();

        messageSenderService.sendWhatsAppMessage(cleanPhone, replyToPatient, clinic);

        if (appointment.quartzJobId != null) {
            try {
                appointmentSchedulerService.cancelSchedule(appointment.quartzJobId);
                System.out.println("[QUARTZ] Agendamento cancelado para consulta " + appointment.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private List<String> generatePhoneVariations(String phone) {
        if (phone.startsWith("55") && (phone.length() == 12 || phone.length() == 13)) {
            String ddd = phone.substring(2, 4);
            String number = phone.substring(4);

            if (number.length() == 8) {
                return Arrays.asList(phone, "55" + ddd + "9" + number);
            } else if (number.length() == 9 && number.startsWith("9")) {
                return Arrays.asList(phone, "55" + ddd + number.substring(1));
            }
        }
        return Arrays.asList(phone);
    }
}