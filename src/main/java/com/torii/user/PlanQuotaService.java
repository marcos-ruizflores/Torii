package com.torii.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Cuotas mensuales por plan: la pieza que convierte los planes de la página
 * /planes en una regla real.
 *
 * <p>ANTES de ejecutar una búsqueda se llama a {@link #consume}: si al usuario no
 * le quedan consultas este mes, se rechaza con 429 (y no se gasta ni una llamada a
 * las APIs externas). Las búsquedas anónimas no pasan por aquí — no hay identidad
 * a la que contar (limitarlas por IP es una mejora futura).
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

    /** Estado de cuota del usuario este mes, para /api/me/usage y el frontend. */
    public record Usage(String plan, Integer limit, int used, LocalDate month) {}

    /**
     * Comprueba y descuenta {@code queries} consultas de la cuota del mes.
     *
     * @throws ResponseStatusException 429 si el plan no tiene cuota suficiente
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
