package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de la caché de Torii, externalizada a {@code application.properties}
 * bajo el prefijo {@code torii.cache}.
 *
 * <p>Antes, el tamaño de la caché y los TTLs estaban escritos a fuego en el código.
 * Sacarlos aquí permite ajustarlos sin recompilar (por entorno, por perfil, por
 * variable de entorno) y deja un único sitio donde ver y tocar todos esos números.
 *
 * <p>Es un {@code record} con enlace por constructor: Spring rellena cada campo
 * desde las propiedades y, si alguna falta, usa el {@link DefaultValue} indicado.
 * Por eso la aplicación funciona aunque no se configure nada.
 *
 * <p>Las duraciones se escriben con sufijo: {@code 7d}, {@code 24h}, {@code 1h},
 * {@code 10m}. Spring las convierte solo a {@link Duration}.
 */
@ConfigurationProperties(prefix = "torii.cache")
public record CacheProperties(

        /** Máximo de entradas en caché; al superarlo, expulsa las menos usadas. */
        @DefaultValue("50000") long maximumSize,

        /** Política de caducidad variable según la cercanía del viaje. */
        @DefaultValue Ttl ttl
) {

    /**
     * Tramos de TTL. {@code *ThresholdDays} son las fronteras en días hasta la salida;
     * los otros campos, cuánto vive la entrada en cada tramo.
     */
    public record Ttl(
            @DefaultValue("60") int farThresholdDays,
            @DefaultValue("14") int mediumThresholdDays,
            @DefaultValue("2")  int nearThresholdDays,

            @DefaultValue("7d")  Duration far,       // salida a > farThresholdDays
            @DefaultValue("24h") Duration medium,    // >= mediumThresholdDays
            @DefaultValue("1h")  Duration near,      // >= nearThresholdDays
            @DefaultValue("10m") Duration imminent   // < nearThresholdDays (o ya pasado)
    ) {}

    /**
     * Instancia con todos los valores por defecto. Útil en tests, que no levantan el
     * contexto de Spring y necesitan construir las propiedades a mano.
     */
    public static CacheProperties defaults() {
        return new CacheProperties(
                50_000,
                new Ttl(60, 14, 2,
                        Duration.ofDays(7), Duration.ofHours(24),
                        Duration.ofHours(1), Duration.ofMinutes(10)));
    }
}
