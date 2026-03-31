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

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    @Transactional
    public void processIncomingMessage(String phone, String text) {
        IntentExtractionResult result = intentExtractionService.extractIntent(text);

        switch (result.action()) {
            case CONFIRM:
                String cleanPhone = phone.replace("@s.whatsapp.net", "");
                Patient patient = Patient.find("whatsappPhone", cleanPhone).firstResult();
                if (patient != null) {
                    Appointment appointment = Appointment.find("patient = ?1 and status = 'PENDING'", patient).firstResult();
                    if (appointment != null) {
                        appointment.status = AppointmentStatus.CONFIRMED;
                        appointment.persist();
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
                    Appointment appointmentCancel = Appointment.find("patient = ?1 and status = 'PENDING'", patientCancel).firstResult();
                    if (appointmentCancel != null) {
                        appointmentCancel.status = AppointmentStatus.CANCELED;
                        appointmentCancel.persist();
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
