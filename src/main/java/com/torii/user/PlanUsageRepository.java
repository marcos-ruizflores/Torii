package com.torii.user;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla {@code plan_usage} (clave compuesta usuario+mes). */
public interface PlanUsageRepository extends JpaRepository<PlanUsage, PlanUsage.Key> {
}
