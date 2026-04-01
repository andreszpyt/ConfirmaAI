package com.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;

@RegisterRestClient(configKey = "evolution-api")
public interface EvolutionApiClient {

    @POST
    @Path("/message/sendText/{instance}")
    void sendMessage(@HeaderParam("apikey") String apiKey, @PathParam("instance") String instance, MessageRequest request);

    @POST
    @Path("/instance/create")
    CreateInstanceResponse createInstance(@HeaderParam("apikey") String globalApiKey, CreateInstanceRequest request);

    record MessageRequest(String number, String text, int delay) {}
    record CreateInstanceRequest(String instanceName, String token, boolean qrcode) {}
    record CreateInstanceResponse(InstanceData instance, String hash) {
        public record InstanceData(String instanceName, String status) {}
    }
}