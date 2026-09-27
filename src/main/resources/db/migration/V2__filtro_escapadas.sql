-- Filtro de escapadas: días de la semana de salida y vuelta (ej. FRIDAY → SUNDAY).
-- NULL en ambas = búsqueda normal por duración (todas las búsquedas anteriores a
-- esta migración). Se guardan para poder REPETIR la búsqueda tal cual se hizo.
-- Dos sentencias separadas en vez de un ALTER con varias columnas: así la sintaxis
-- vale igual en PostgreSQL (producción) y en H2 (tests).
ALTER TABLE searches ADD COLUMN depart_day_of_week VARCHAR(10);
ALTER TABLE searches ADD COLUMN return_day_of_week VARCHAR(10);
