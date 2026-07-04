package com.torii.model;

/**
 * Precisión (granularidad) de la búsqueda: cada cuántos días se desliza la fecha de
 * salida al explorar el rango de vacaciones.
 *
 * <p>Es el mando que controla el equilibrio entre <b>coste</b> (número de consultas
 * a la fuente de datos) y <b>cobertura</b> (probabilidad de encontrar la mejor
 * oferta absoluta). Saltar días reduce las consultas, pero puede dejar fuera el día
 * exacto más barato.
 *
 * <p>Pensado también para el futuro modelo de negocio:
 * <ul>
 *   <li>{@link #FAST}: versión gratuita — rápida y barata, buena pero no óptima.</li>
 *   <li>{@link #BALANCED}: término medio.</li>
 *   <li>{@link #EXHAUSTIVE}: versión de pago — explora día a día, máxima cobertura.</li>
 * </ul>
 */
public enum SearchPrecision {

    FAST(3),
    BALANCED(2),
    EXHAUSTIVE(1);

    private final int dayStep;

    SearchPrecision(int dayStep) {
        this.dayStep = dayStep;
    }

    /** Días que avanza la fecha de salida en cada iteración del algoritmo. */
    public int dayStep() {
        return dayStep;
    }
}
