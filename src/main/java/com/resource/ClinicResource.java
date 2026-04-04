package com.resource;

import com.domain.Clinic;
import com.client.EvolutionApiClient;
import com.client.EvolutionApiClient.CreateInstanceRequest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
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

    @GET
    @Path("/{id}/qrcode")
    public Response getQrCode(@PathParam("id") Long id) {
        Clinic clinic = Clinic.findById(id);
        if (clinic == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Clínica não encontrada").build();
        }

        try {
            var response = evolutionApi.connectInstance(globalApiKey, clinic.instanceName);
            if (response != null && response.base64() != null) {
                return Response.ok(response.base64()).build();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity("QR Code já lido ou instância desconectada.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar QR Code: " + e.getMessage()).build();
        }
    }

    @POST
    @Transactional
    public Response createClinic(Clinic clinicInput) {

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

    @GET
    @Path("/{id}/connect-whatsapp")
    public Response connectWhatsapp(@PathParam("id") Long id) {
        Clinic clinic = Clinic.findById(id);
        if (clinic == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Clínica não encontrada").build();
        }

        try {
            var request = new EvolutionApiClient.CreateInstanceRequest(
                    clinic.instanceName,
                    clinic.evolutionApiToken,
                    true
            );

            var apiResponse = evolutionApi.createInstance(globalApiKey, request);

            if (apiResponse != null && apiResponse.qrcode() != null) {
                return Response.ok(apiResponse.qrcode()).build();
            }

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("A instância já pode estar conectada ou a API não retornou o QR Code.")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao conectar com Evolution API: " + e.getMessage())
                    .build();
        }
    }
}