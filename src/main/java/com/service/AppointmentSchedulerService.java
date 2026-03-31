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
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import com.domain.Appointment;
import com.job.SendWhatsAppMessageJob;

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

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + appointment.id)
                .startAt(DateBuilder.futureDate(1, IntervalUnit.MINUTE))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        appointment.quartzJobId = jobDetail.getKey().getName();
        appointment.persist();
    }

    public void cancelSchedule(String jobId) throws Exception {
        JobKey jobKey = new JobKey(jobId);
        scheduler.deleteJob(jobKey);
        System.out.println("[QUARTZ] Agendamento cancelado: " + jobId);
    }
}

