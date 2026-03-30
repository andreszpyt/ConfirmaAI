package com.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Patient extends PanacheEntity {

    public String name;
    public String whatsappPhone;

}