package com.resource;

import com.domain.Appointment;
import com.domain.Appointment.AppointmentStatus;
import com.domain.Clinic;
import com.domain.Patient;
import com.service.AppointmentSchedulerService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.PartType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

@Path("/appointments")
public class AppointmentResource {

    @Inject
    AppointmentSchedulerService appointmentSchedulerService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadCSV(@FormParam("file") InputStream file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file))) {

            String line;
            int count = 0;
            boolean isHeader = true;

            Clinic clinic = Clinic.findById(1L);
            if (clinic == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Clinic with ID 1 not found")
                        .build();
            }

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }

                String patientName = parts[0].trim();
                String whatsappPhone = parts[1].trim();
                String scheduledAtStr = parts[2].trim();

                // Buscar ou criar Patient
                Patient patient = Patient.find("whatsappPhone", whatsappPhone).firstResult();
                if (patient == null) {
                    patient = new Patient();
                    patient.name = patientName;
                    patient.whatsappPhone = whatsappPhone;
                    patient.persist();
                }

                // Criar Appointment
                Appointment appointment = new Appointment();
                appointment.clinic = clinic;
                appointment.patient = patient;
                appointment.status = AppointmentStatus.PENDING;
                appointment.scheduledAt = LocalDateTime.parse(scheduledAtStr);
                appointment.persist();

                // Agendar confirmação
                try {
                    appointmentSchedulerService.scheduleConfirmation(appointment);
                } catch (Exception e) {
                    System.err.println("Erro ao agendar confirmação para appointment " + appointment.id + ": " + e.getMessage());
                }

                count++;
            }

            return Response.ok("CSV importado com sucesso. " + count + " consultas criadas.").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar CSV: " + e.getMessage())
                    .build();
        }
    }
}

