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
import java.time.LocalDateTime;
import java.time.DayOfWeek;
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

        LocalDateTime dispatchTime = calculateDispatchTime(appointment.scheduledAt);
        Date dispatchDate = java.sql.Timestamp.valueOf(dispatchTime);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + appointment.id)
                .startAt(dispatchDate)
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

    private LocalDateTime calculateDispatchTime(LocalDateTime appointmentTime) {
        // Subtrair 4 horas da consulta
        LocalDateTime dispatchTime = appointmentTime.minusHours(4);

        // Se for antes das 08:00, mude para 08:00 do mesmo dia
        if (dispatchTime.getHour() < 8) {
            dispatchTime = dispatchTime.withHour(8).withMinute(0).withSecond(0);
        }

        // Se for após 18:00, mude para 18:00 do dia anterior
        if (dispatchTime.getHour() >= 18) {
            dispatchTime = dispatchTime.minusDays(1).withHour(18).withMinute(0).withSecond(0);
        }

        // Se cair em domingo, mova para segunda-feira às 08:00
        if (dispatchTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dispatchTime = dispatchTime.plusDays(1).withHour(8).withMinute(0).withSecond(0);
        }

        return dispatchTime;
    }
}

