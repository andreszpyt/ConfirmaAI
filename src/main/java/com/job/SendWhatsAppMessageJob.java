package com.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.domain.Appointment;

public class SendWhatsAppMessageJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String appointmentId = context.getJobDetail().getJobDataMap().getString("appointmentId");
        Appointment appointment = Appointment.findById(Long.parseLong(appointmentId));
        if (appointment != null) {
            System.out.println("[QUARTZ] Disparando WhatsApp para " + appointment.patient.whatsappPhone + ": Olá " + appointment.patient.name + ", você confirma sua consulta? Responda Sim ou Não");
        }
    }
}
