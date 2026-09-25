package com.torii.user;

import org.springframework.data.jpa.repository.JpaRepository;

/** Access to {@code plan_usage} (composite user + month key). */
public interface PlanUsageRepository extends JpaRepository<PlanUsage, PlanUsage.Key> {
}
