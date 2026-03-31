package com.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Clinic extends PanacheEntity {

    public String name;
    public String whatsappPhone;
    public String evolutionApiToken;
    public String instanceName;
    public String webhookToken;
    public boolean active = true;

}