package br.com.confirmaai.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.domain.Appointment;
import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.MessageRequest;

public class SendWhatsAppMessageJob implements Job {

    @RestClient
    EvolutionApiClient evolutionApiClient;

    @ConfigProperty(name = "evolution-api.token")
    String token;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String appointmentId = context.getJobDetail().getJobDataMap().getString("appointmentId");
        Appointment appointment = Appointment.findById(Long.parseLong(appointmentId));
        if (appointment != null) {
            String message = "Olá " + appointment.patient.name + ", você confirma sua consulta? Responda Sim ou Não";
            MessageRequest request = new MessageRequest(appointment.patient.whatsappPhone, message, 0);
            evolutionApiClient.sendMessage(token, "instancia-mvp", request);
        }
    }
}
