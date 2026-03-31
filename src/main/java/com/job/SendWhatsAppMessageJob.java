package com.job;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.domain.Appointment;

@ApplicationScoped
public class SendWhatsAppMessageJob implements Job {

    @Override
    @Transactional
    @ActivateRequestContext
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String appointmentId = context.getJobDetail().getJobDataMap().getString("appointmentId");
        Appointment appointment = Appointment.findById(Long.parseLong(appointmentId));
        if (appointment != null) {
            System.out.println("[QUARTZ] Disparando WhatsApp para " + appointment.patient.whatsappPhone + ": Olá " + appointment.patient.name + ", você confirma sua consulta? Responda Sim ou Não");
        }
    }
}
