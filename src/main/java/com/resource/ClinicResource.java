package com.resource;

import com.domain.Clinic;
import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.CreateInstanceRequest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.UUID;

@Path("/api/clinics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClinicResource {

    @Inject
    @org.eclipse.microprofile.rest.client.inject.RestClient
    EvolutionApiClient evolutionApi;

    @ConfigProperty(name = "evolution-api.token")
    String globalApiKey;

    @POST
    @Transactional
    public Response createClinic(Clinic clinicInput) {

        clinicInput.webhookToken = UUID.randomUUID().toString();
        clinicInput.evolutionApiToken = UUID.randomUUID().toString();

        String safeName = clinicInput.name.toLowerCase().replaceAll("[^a-z0-9]", "");
        clinicInput.instanceName = safeName + "-" + UUID.randomUUID().toString().substring(0, 5);

        clinicInput.persist();

        try {
            CreateInstanceRequest request = new CreateInstanceRequest(clinicInput.instanceName, clinicInput.evolutionApiToken, true);
            evolutionApi.createInstance(globalApiKey, request);

            return Response.status(Response.Status.CREATED).entity(clinicInput).build();

        } catch (Exception e) {
            System.err.println("Erro ao criar instância na Evolution API: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Clínica salva, mas falha ao criar instância no WhatsApp: " + e.getMessage())
                    .build();
        }
    }
}