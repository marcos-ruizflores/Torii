package com.torii.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Monthly quotas per plan. This is what turns the plans on the /planes page into an
 * actual rule.
 *
 * <p>{@link #consume} is called BEFORE running a search: if the user has no lookups
 * left this month it's rejected with a 429, without spending a single external API
 * call. Anonymous searches skip this since there's no identity to count against
 * (rate limiting by IP is a TODO).
 */
@Service
public class PlanQuotaService {

    private final UserRepository users;
    private final PlanUsageRepository usages;
    private final Clock clock;

    public PlanQuotaService(UserRepository users, PlanUsageRepository usages, Clock clock) {
        this.users = users;
        this.usages = usages;
        this.clock = clock;
    }

    /** User's quota status for this month, used by /api/me/usage and the frontend. */
    public record Usage(String plan, Integer limit, int used, LocalDate month) {}

    /**
     * Checks and subtracts {@code queries} lookups from this month's quota.
     *
     * @throws ResponseStatusException 429 if the plan doesn't have enough quota left
     */
    @Transactional
    public void consume(Long userId, int queries) {
        Plan plan = planOf(userId);
        PlanUsage usage = currentMonthUsage(userId);

        Integer limit = plan.monthlyQueries();
        if (limit != null && usage.getQueriesUsed() + queries > limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Esta búsqueda necesita %d consultas y solo te quedan %d este mes (plan %s, límite %d). Reduce el rango, usa precisión Rápida o mejora tu plan."
                            .formatted(queries, Math.max(0, limit - usage.getQueriesUsed()), plan, limit));
        }

        usage.add(queries);
        usages.save(usage);
    }

    @Transactional(readOnly = true)
    public Usage usageOf(Long userId) {
        Plan plan = planOf(userId);
        PlanUsage usage = currentMonthUsage(userId);
        return new Usage(plan.name(), plan.monthlyQueries(),
                usage.getQueriesUsed(), usage.getKey().usageMonth());
    }

    private Plan planOf(Long userId) {
        return users.findById(userId)
                .map(u -> Plan.fromName(u.getPlan()))
                .orElse(Plan.FREE);
    }

    private PlanUsage currentMonthUsage(Long userId) {
        LocalDate month = LocalDate.now(clock).withDayOfMonth(1);
        return usages.findById(new PlanUsage.Key(userId, month))
                .orElseGet(() -> new PlanUsage(userId, month));
    }
}
