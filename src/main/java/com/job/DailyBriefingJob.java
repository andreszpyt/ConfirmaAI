package com.job;

import com.domain.Appointment;
import com.domain.Clinic;
import com.service.MessageSenderService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class DailyBriefingJob {

    @Inject
    MessageSenderService messageSenderService;

    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional
    public void sendMorningBriefing() {
        List<Clinic> activeClinics = Clinic.list("active", true);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        for (Clinic clinic : activeClinics) {
            List<Appointment> todaysAppointments = Appointment.find("clinic = ?1 and scheduledAt between ?2 and ?3",
                    clinic, startOfDay, endOfDay).list();

            if (todaysAppointments.isEmpty()) {
                continue;
            }

            long confirmed = todaysAppointments.stream().filter(a -> a.status == Appointment.AppointmentStatus.CONFIRMED).count();
            long canceled = todaysAppointments.stream().filter(a -> a.status == Appointment.AppointmentStatus.CANCELED).count();
            long pending = todaysAppointments.stream().filter(a -> a.status == Appointment.AppointmentStatus.PENDING).count();

            String report = String.format("""
                Bom dia! ☀️ Aqui está o status da agenda de hoje:
                ✅ %d Confirmados
                ❌ %d Cancelados (vagos)
                ⏳ %d Aguardando resposta
                """, confirmed, canceled, pending);

            messageSenderService.sendWhatsAppMessage(clinic.whatsappPhone, report);
            System.out.println("[DAILY BRIEFING] Relatório enviado para a clínica " + clinic.name);
        }
    }
}