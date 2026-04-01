package com.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.quartz.Scheduler;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.JobKey;
import org.quartz.JobBuilder;
import org.quartz.TriggerBuilder;
import com.domain.Appointment;
import com.job.SendWhatsAppMessageJob;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
public class AppointmentSchedulerService {

    @Inject
    Scheduler scheduler;

    @Transactional
    public void scheduleConfirmation(Appointment appointment) throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(SendWhatsAppMessageJob.class)
                .withIdentity("appointment_" + appointment.id)
                .usingJobData("appointmentId", String.valueOf(appointment.id))
                .build();

        int leadTime = appointment.clinic.confirmationLeadTimeHours != null ? appointment.clinic.confirmationLeadTimeHours : 24;

        LocalDateTime targetDispatchTime = appointment.scheduledAt.minusHours(leadTime);

        if (targetDispatchTime.isBefore(LocalDateTime.now())) {
            System.out.println("Aviso: Upload atrasado. Agendando disparo imediato para consulta " + appointment.id);
            targetDispatchTime = LocalDateTime.now().plusMinutes(1);
        }

        Date dispatchDate = Date.from(targetDispatchTime.atZone(ZoneId.systemDefault()).toInstant());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + appointment.id)
                .startAt(dispatchDate)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        appointment.quartzJobId = jobDetail.getKey().getName();
        appointment.persist();

        System.out.println("[QUARTZ] Agendado disparo da consulta " + appointment.id + " para: " + targetDispatchTime);
    }

    public void cancelSchedule(String jobId) throws Exception {
        if (jobId != null) {
            JobKey jobKey = new JobKey(jobId);
            scheduler.deleteJob(jobKey);
            System.out.println("[QUARTZ] Agendamento cancelado: " + jobId);
        }
    }
}