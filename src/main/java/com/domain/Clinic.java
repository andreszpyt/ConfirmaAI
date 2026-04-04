package com.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@Entity
public class Clinic extends PanacheEntity {

    public String name;
    public String whatsappPhone;
    public String evolutionApiToken;
    public String instanceName;

    @Column(unique = true, nullable = false)
    public String webhookToken;

    public boolean active = true;
    public Integer confirmationLeadTimeHours = 24;

    @Column(length = 500)
    public String msgTemplateConfirm = "Ótimo! Sua consulta está confirmada. Nos vemos em breve.";

    @Column(length = 500)
    public String msgTemplateCancel = "Tudo bem, sua consulta foi cancelada. Obrigado por avisar!";

    @Column(length = 500)
    public String msgTemplateReschedule = "Certo, um de nossos atendentes vai falar com você em instantes para encontrarmos um novo horário.";

    @PrePersist
    public void prePersist() {
        if (this.webhookToken == null) {
            this.webhookToken = UUID.randomUUID().toString();
        }
        if (this.evolutionApiToken == null) {
            this.evolutionApiToken = UUID.randomUUID().toString();
        }
    }
}