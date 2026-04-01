package com.service;

import com.domain.Appointment;
import com.domain.Clinic;
import com.domain.Patient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class CsvProcessorService {

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public int processAgendaCsv(byte[] csvData, Clinic clinic) {
        int successCount = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvData), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length < 4) continue;

                String name = columns[0].trim();
                String phone = columns[1].trim();
                String dateStr = columns[2].trim();
                String timeStr = columns[3].trim();

                Patient patient = Patient.find("whatsappPhone", phone).firstResult();
                if (patient == null) {
                    patient = new Patient();
                    patient.name = name;
                    patient.whatsappPhone = phone;
                    patient.persist();
                }

                Appointment appointment = new Appointment();
                appointment.clinic = clinic;
                appointment.patient = patient;
                appointment.status = Appointment.AppointmentStatus.PENDING;
                appointment.scheduledAt = LocalDateTime.parse(dateStr + " " + timeStr, formatter);
                appointment.persist();

                appointmentSchedulerService.scheduleConfirmation(appointment);
                successCount++;
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar CSV: " + e.getMessage());
        }

        return successCount;
    }
}