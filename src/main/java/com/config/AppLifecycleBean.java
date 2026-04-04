package com.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import java.util.TimeZone;

@ApplicationScoped
public class AppLifecycleBean {

    void onStart(@Observes StartupEvent ev) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        System.out.println("[CONFIG] Confirma AI iniciado. TimeZone definido para: " + TimeZone.getDefault().getID());
    }
}