package br.com.ai;

import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;

@RegisterAiService
public interface IntentExtractionService {

    @SystemMessage("You are a medical scheduling assistant. Extract the intent from the user's message: determine the action (CONFIRM, CANCEL, RESCHEDULE, ADD, UNKNOWN), originalTime, newTime, and patientName. Return only the JSON equivalent to IntentExtractionResult.")
    IntentExtractionResult extractIntent(String message);

    record IntentExtractionResult(Action action, String originalTime, String newTime, String patientName) {

        public enum Action {
            CONFIRM, CANCEL, RESCHEDULE, ADD, UNKNOWN
        }

    }

}
