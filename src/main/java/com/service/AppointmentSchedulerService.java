package com.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.quartz.*;
import com.domain.Appointment;
import com.job.SendWhatsAppMessageJob;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
public class AppointmentSchedulerService {

    private static final Logger LOG = Logger.getLogger(AppointmentSchedulerService.class);
    private static final ZoneId ZONE_ID = ZoneId.of("America/Fortaleza");

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

        if (targetDispatchTime.isBefore(LocalDateTime.now(ZONE_ID))) {
            LOG.warnf("Aviso: Upload atrasado. Agendando disparo imediato para consulta %d", appointment.id);
            targetDispatchTime = LocalDateTime.now(ZONE_ID).plusMinutes(1);
        }

        Date dispatchDate = Date.from(targetDispatchTime.atZone(ZONE_ID).toInstant());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + appointment.id)
                .startAt(dispatchDate)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        appointment.quartzJobId = jobDetail.getKey().getName();
        appointment.persist();

        LOG.infof("[QUARTZ] Agendado disparo da consulta %d para: %s", appointment.id, targetDispatchTime);
    }

    public void cancelSchedule(String jobId) throws Exception {
        if (jobId != null) {
            scheduler.deleteJob(new JobKey(jobId));
            LOG.infof("[QUARTZ] Agendamento cancelado: %s", jobId);
        }
    }
}