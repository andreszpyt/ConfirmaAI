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
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Arrays;

@ApplicationScoped
public class MessageProcessorService {

    private static final Logger LOG = Logger.getLogger(MessageProcessorService.class);

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
            LOG.warnf("Paciente não encontrado para o número (ou variações): %s", cleanPhone);
            return;
        }

        Appointment appointment = Appointment.find(
                "patient = ?1 and clinic = ?2 and status = 'PENDING' ORDER BY scheduledAt ASC",
                patient, clinic).firstResult();

        if (appointment == null) {
            LOG.infof("Nenhuma consulta PENDENTE encontrada para o paciente: %s", patient.name);
            return;
        }

        IntentExtractionResult result = null;

        try {
            result = intentExtractionService.extractIntent(text);
        } catch (Exception e) {
            LOG.errorf(e, "Falha na IA ao extrair intenção para o paciente %s. Acionando fallback.", patient.name);

            appointment.status = AppointmentStatus.NEEDS_HUMAN;
            appointment.persist();

            String fallbackReply = "Desculpe, nosso sistema está passando por uma instabilidade momentânea. Por favor, responda apenas com *SIM* para confirmar ou *NÃO* para cancelar a sua consulta.";
            messageSenderService.sendWhatsAppMessage(cleanPhone, fallbackReply, clinic);

            if (clinic.whatsappPhone != null) {
                messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone,
                        "⚠️ *ALERTA DO SISTEMA*: A IA falhou ao processar a resposta do paciente *" + patient.name + "*. Verifique o chat e atualize o status manualmente.", clinic);
            }
            return;
        }

        String replyToPatient = "";

        switch (result.action()) {
            case CONFIRM:
                appointment.status = AppointmentStatus.CONFIRMED;
                replyToPatient = clinic.msgTemplateConfirm;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "✅ Notificação: Consulta do paciente *" + patient.name + "* foi CONFIRMADA.", clinic);
                }
                break;
            case CANCEL:
                appointment.status = AppointmentStatus.CANCELED;
                replyToPatient = clinic.msgTemplateCancel;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "❌ Notificação: Consulta do paciente *" + patient.name + "* foi CANCELADA.", clinic);
                }
                break;
            case RESCHEDULE:
                appointment.status = AppointmentStatus.NEEDS_HUMAN;
                replyToPatient = clinic.msgTemplateReschedule;
                if (clinic.whatsappPhone != null) {
                    messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, "⚠️ ALERTA: O paciente *" + patient.name + "* solicitou REAGENDAMENTO.", clinic);
                }
                break;
            default:
                LOG.infof("Intenção não reconhecida com segurança para o paciente %s. Mantendo status atual.", patient.name);
                return;
        }

        appointment.persist();

        messageSenderService.sendWhatsAppMessage(cleanPhone, replyToPatient, clinic);

        if (appointment.quartzJobId != null) {
            try {
                appointmentSchedulerService.cancelSchedule(appointment.quartzJobId);
                LOG.infof("[QUARTZ] Agendamento cancelado para consulta %d", appointment.id);
            } catch (Exception e) {
                LOG.errorf(e, "Falha ao cancelar agendamento Quartz para consulta %d", appointment.id);
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