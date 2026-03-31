package com.ai;

import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;

@RegisterAiService
public interface IntentExtractionService {

    @SystemMessage("You are a professional medical assistant. Analyze the user's message and extract: action (CONFIRM, CANCEL, RESCHEDULE, ADD, UNKNOWN). If the user says things like \"Vou sim\", \"Confirmado\", \"Com certeza\", \"Pode marcar\", it is a CONFIRM. If they say \"Não vou\", \"Cancele\", \"Infelizmente não dá\", it is a CANCEL. If they mention a new date or time, it is a RESCHEDULE. Return only valid JSON.")
    IntentExtractionResult extractIntent(String message);

    record IntentExtractionResult(Action action, String originalTime, String newTime, String patientName) {

        public enum Action {
            CONFIRM, CANCEL, RESCHEDULE, ADD, UNKNOWN
        }

    }

}
