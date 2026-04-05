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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        // Panache/Hibernate expande a coleção em parâmetros nomeados para JPQL "in ?1"
        List<Patient> patients = Patient.find("whatsappPhone in ?1", phoneVariations).list();

        if (patients.isEmpty()) {
            LOG.warnf("Paciente não encontrado para o número (ou variações): %s", cleanPhone);
            return;
        }

        if (patients.size() > 1) {
            LOG.errorf(
                    "Múltiplos pacientes inesperados para as mesmas variações de WhatsApp (esperado no máximo 1). "
                            + "cleanPhone=%s, phoneVariations=%s, clinicId=%d, matchCount=%d, detalhes=[%s]",
                    cleanPhone,
                    phoneVariations,
                    clinic.id,
                    patients.size(),
                    patients.stream()
                            .map(p -> String.format("id=%d,name=%s,whatsappPhone=%s", p.id, p.name, p.whatsappPhone))
                            .collect(Collectors.joining("; ")));
        }

        Patient patient = patients.getFirst();

        List<Appointment> pendingAppointments = Appointment.find(
                "patient = ?1 and clinic = ?2 and status = ?3 order by scheduledAt asc",
                patient,
                clinic,
                AppointmentStatus.PENDING).list();

        if (pendingAppointments.isEmpty()) {
            LOG.infof("Nenhuma consulta PENDENTE encontrada para o paciente: %s", patient.name);
            return;
        }

        if (pendingAppointments.size() > 1) {
            LOG.errorf(
                    "Múltiplas consultas PENDING inesperadas para o mesmo paciente e clínica (esperado no máximo 1 ativa por fluxo). "
                            + "patientId=%d, patientName=%s, clinicId=%d, matchCount=%d, detalhes=[%s]",
                    patient.id,
                    patient.name,
                    clinic.id,
                    pendingAppointments.size(),
                    pendingAppointments.stream()
                            .map(a -> String.format(
                                    "id=%d,scheduledAt=%s,status=%s",
                                    a.id,
                                    a.scheduledAt,
                                    a.status))
                            .collect(Collectors.joining("; ")));
        }

        Appointment appointment = pendingAppointments.getFirst();

        IntentExtractionResult result = null;

        try {
            result = intentExtractionService.extractIntent(text);
        } catch (Exception e) {
            LOG.errorf(e, "Falha na IA ao extrair intenção para o paciente %s. Acionando fallback.", patient.name);

            appointment.status = AppointmentStatus.NEEDS_HUMAN;
            appointment.persist();

            String fallbackReply = "Desculpe, nosso sistema está passando por uma instabilidade momentânea. Por favor, responda apenas com *SIM* para confirmar ou *NÃO* para cancelar a sua consulta.";
            try {
                messageSenderService.sendWhatsAppMessageThrowingOnFailure(cleanPhone, fallbackReply, clinic);
            } catch (Exception sendEx) {
                LOG.fatal(
                        "Falha Crítica de Comunicação: envio da mensagem de fallback ao paciente falhou após retentativas.",
                        sendEx);
                appointment.communicationObservation = "Falha Crítica de Comunicação";
                appointment.persist();
            }

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