package com.torii.user;

/**
 * Los planes de Torii y su cuota mensual de consultas. Debe cuadrar con lo que
 * promete la página /planes del frontend.
 *
 * <p>Recuerda: una BÚSQUEDA consume varias CONSULTAS (una por par de fechas
 * explorado); el estimador del formulario enseña el coste antes de buscar.
 */
public enum Plan {
    FREE(30),
    PRO(500),
    BUSINESS(null); // null = sin límite

    private final Integer monthlyQueries;

    Plan(Integer monthlyQueries) {
        this.monthlyQueries = monthlyQueries;
    }

    /** Cuota mensual de consultas, o null si es ilimitada. */
    public Integer monthlyQueries() {
        return monthlyQueries;
    }

    /** Tolerante con datos viejos: un plan desconocido en BD se trata como FREE. */
    public static Plan fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return FREE;
        }
    }
}
