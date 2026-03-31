package com.job;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.domain.Appointment;
import com.service.MessageSenderService;

@ApplicationScoped
public class SendWhatsAppMessageJob implements Job {

    MessageSenderService messageSenderService;

    @Override
    @Transactional
    @ActivateRequestContext
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String appointmentId = context.getJobDetail().getJobDataMap().getString("appointmentId");
        Appointment appointment = Appointment.findById(Long.parseLong(appointmentId));
        if (appointment != null) {
            String message = "Olá " + appointment.patient.name + ", você confirma sua consulta na " + appointment.clinic.name + " amanhã?";
            messageSenderService.sendWhatsAppMessage(appointment.patient.whatsappPhone, message);
        }
    }
}
