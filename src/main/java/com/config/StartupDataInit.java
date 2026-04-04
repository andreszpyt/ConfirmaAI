package com.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import com.domain.Clinic;
import com.domain.Patient;
import com.domain.Appointment;
import com.service.AppointmentSchedulerService;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
@UnlessBuildProfile("prod")
public class StartupDataInit {

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (Clinic.count() == 0) {
            Clinic clinic = new Clinic();
            clinic.name = "Clínica MVP";
            clinic.whatsappPhone = "5585999999999";
            clinic.persist();

            Patient patient = new Patient();
            patient.name = "Paciente Teste";
            patient.whatsappPhone = "5585999999999";
            patient.persist();

            Appointment appointment = new Appointment();
            appointment.clinic = clinic;
            appointment.patient = patient;
            appointment.scheduledAt = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
            appointment.status = Appointment.AppointmentStatus.PENDING;
            appointment.persist();

            try {
                appointmentSchedulerService.scheduleConfirmation(appointment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}