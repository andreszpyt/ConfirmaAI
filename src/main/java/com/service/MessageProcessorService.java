package com.service;

import com.ai.IntentExtractionService;
import com.ai.IntentExtractionService.IntentExtractionResult;
import com.domain.Clinic;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.domain.Patient;
import com.domain.Appointment;
import com.domain.Appointment.AppointmentStatus;

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
        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        switch (result.action()) {
            case CONFIRM:
                String cleanPhone = phone.replace("@s.whatsapp.net", "");
                Patient patient = Patient.find("whatsappPhone", cleanPhone).firstResult();
                if (patient != null) {
                    Appointment appointment = Appointment.find("patient.whatsappPhone = ?1 and clinic = ?2 and status = 'PENDING'", cleanPhone, clinic).firstResult();
                    if (appointment != null) {
                        appointment.status = AppointmentStatus.CONFIRMED;
                        appointment.persist();
                        messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "Notificação: Consulta do paciente " + patient.name + " foi " + result.action());
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
                break;
            case CANCEL:
                String cleanPhoneCancel = phone.replace("@s.whatsapp.net", "");
                Patient patientCancel = Patient.find("whatsappPhone", cleanPhoneCancel).firstResult();
                if (patientCancel != null) {
                    Appointment appointmentCancel = Appointment.find("patient.whatsappPhone = ?1 and clinic = ?2 and status = 'PENDING'", cleanPhoneCancel, clinic).firstResult();
                    if (appointmentCancel != null) {
                        appointmentCancel.status = AppointmentStatus.CANCELED;
                        appointmentCancel.persist();
                        messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "Notificação: Consulta do paciente " + patientCancel.name + " foi " + result.action());
                        if (appointmentCancel.quartzJobId != null) {
                            try {
                                appointmentSchedulerService.cancelSchedule(appointmentCancel.quartzJobId);
                                System.out.println("[QUARTZ] Agendamento cancelado para consulta " + appointmentCancel.id);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            case RESCHEDULE:
                String cleanPhoneReschedule = phone.replace("@s.whatsapp.net", "");
                Patient patientReschedule = Patient.find("whatsappPhone", cleanPhoneReschedule).firstResult();
                if (patientReschedule != null) {
                    Appointment appointmentReschedule = Appointment.find("patient.whatsappPhone = ?1 and clinic = ?2 and status = 'PENDING'", cleanPhoneReschedule, clinic).firstResult();
                    if (appointmentReschedule != null) {
                        appointmentReschedule.status = AppointmentStatus.NEEDS_HUMAN;
                        appointmentReschedule.persist();
                        messageSenderService.sendWhatsAppMessage(CLINIC_PHONE, "ALERTA: O paciente " + patientReschedule.name + " solicitou um reagendamento. Por favor, assuma o atendimento manual.");
                        if (appointmentReschedule.quartzJobId != null) {
                            try {
                                appointmentSchedulerService.cancelSchedule(appointmentReschedule.quartzJobId);
                                System.out.println("[QUARTZ] Agendamento cancelado para consulta " + appointmentReschedule.id);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
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
