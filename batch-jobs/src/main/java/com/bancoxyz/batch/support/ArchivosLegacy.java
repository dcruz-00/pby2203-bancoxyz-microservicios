package com.bancoxyz.batch.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Ubica los CSV del sistema legacy en la carpeta configurada (batch.directorio-datos). */
@Component
public class ArchivosLegacy {

    public static final String MOVIMIENTOS_DIARIOS = "movimientos_financieros_diarios.csv";
    public static final String INTERESES = "intereses_trimestrales.csv";
    public static final String ESTADOS_ANUALES = "estados_financieros_anuales.csv";

    private final ResourceLoader resourceLoader;
    private final String directorio;

    public ArchivosLegacy(ResourceLoader resourceLoader, @Value("${batch.directorio-datos}") String directorio) {
        this.resourceLoader = resourceLoader;
        this.directorio = directorio.endsWith("/") ? directorio : directorio + "/";
    }

    public Resource recurso(String nombre) {
        return resourceLoader.getResource(directorio + nombre);
    }

    public String getDirectorio() {
        return directorio;
    }
}
