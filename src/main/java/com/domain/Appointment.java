package com.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Entity
public class Appointment extends PanacheEntity {

    @ManyToOne
    public Clinic clinic;

    @ManyToOne
    public Patient patient;

    public LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    public AppointmentStatus status = AppointmentStatus.PENDING;

    public String quartzJobId;

    /** Observação operacional (ex.: falha crítica ao notificar o paciente). */
    @Column(length = 512)
    public String communicationObservation;

    public enum AppointmentStatus {
        PENDING,
        CONFIRMED,
        CANCELED,
        NEEDS_HUMAN
    }
}