package com.logging;

import org.jboss.logging.Logger;

/**
 * Eventos de processamento em formato estável (flow=… key=value) para correlação e parsers.
 * Telefone é mascarado para reduzir dados pessoais nos logs; não registrar corpo de mensagens.
 */
public final class StructuredEventLog {

    private StructuredEventLog() {
    }

    public static String maskPhoneDigitsForLog(String digits) {
        if (digits == null || digits.isEmpty()) {
            return "n/a";
        }
        String d = digits.replaceAll("\\D", "");
        if (d.length() <= 4) {
            return "****";
        }
        int tailLen = 4;
        int prefixLen = Math.min(2, d.length() - tailLen);
        int starCount = d.length() - prefixLen - tailLen;
        return d.substring(0, prefixLen) + "*".repeat(Math.max(0, starCount)) + d.substring(d.length() - tailLen);
    }

    public static void messageProcess(
            Logger log,
            String phase,
            Long clinicId,
            String patientPhoneDigits,
            String appointmentIdRef,
            String outcome) {
        String masked = maskPhoneDigitsForLog(patientPhoneDigits);
        if (outcome == null || outcome.isEmpty()) {
            log.infof(
                    "flow=message_process phase=%s clinicId=%d patientPhone=%s appointmentId=%s",
                    phase,
                    clinicId,
                    masked,
                    appointmentIdRef);
        } else {
            log.infof(
                    "flow=message_process phase=%s clinicId=%d patientPhone=%s appointmentId=%s outcome=%s",
                    phase,
                    clinicId,
                    masked,
                    appointmentIdRef,
                    outcome);
        }
    }

    public static void messageProcessIntent(Logger log, String extractedAction) {
        log.infof("flow=message_process phase=intent_extracted extractedAction=%s", extractedAction);
    }

    public static void webhookReceive(
            Logger log,
            String phase,
            Long clinicId,
            String patientPhoneDigits,
            String appointmentIdRef,
            String handler,
            String outcome) {
        log.infof(
                "flow=webhook_receive phase=%s clinicId=%d patientPhone=%s appointmentId=%s handler=%s outcome=%s",
                phase,
                clinicId,
                maskPhoneDigitsForLog(patientPhoneDigits),
                appointmentIdRef,
                handler,
                outcome);
    }
}
