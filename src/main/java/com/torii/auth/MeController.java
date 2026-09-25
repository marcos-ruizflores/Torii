package com.torii.auth;

import com.torii.auth.AuthDtos.UserDto;
import com.torii.history.SavedSearchDto;
import com.torii.history.SearchHistoryService;
import com.torii.user.Plan;
import com.torii.user.PlanQuotaService;
import com.torii.user.UserAccount;
import com.torii.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/**
 * Data for the authenticated user ("me" = whoever owns the token). Everything under
 * {@code /api/me/**} needs a valid token (see SecurityConfig). Spring injects the
 * verified JWT as the principal and its subject is the user id.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;
    private final SearchHistoryService searchHistory;
    private final PlanQuotaService quota;

    public MeController(UserRepository users, SearchHistoryService searchHistory,
                        PlanQuotaService quota) {
        this.users = users;
        this.searchHistory = searchHistory;
        this.quota = quota;
    }

    /** Profile of the token owner, used to restore the session on page reload. */
    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta del token ya no existe"));
    }

    /** User's latest searches, so they can be repeated with one click. */
    @GetMapping("/searches")
    public List<SavedSearchDto> mySearches(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "10") int limit) {
        int clamped = Math.max(1, Math.min(limit, 50));
        return searchHistory.recentSearches(Long.valueOf(jwt.getSubject()), clamped);
    }

    /** This month's quota: plan, limit (null = unlimited) and lookups used so far. */
    @GetMapping("/usage")
    public PlanQuotaService.Usage myUsage(@AuthenticationPrincipal Jwt jwt) {
        return quota.usageOf(Long.valueOf(jwt.getSubject()));
    }

    public record ChangePlanRequest(@NotBlank String plan) {}

    /**
     * Changes the account plan. NO payments yet, it's here so each plan's quota can be
     * tested from the /planes page. Once there's a payment gateway this becomes the
     * payment confirmation step.
     */
    @PostMapping("/plan")
    @Transactional
    public UserDto changePlan(@AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestBody ChangePlanRequest request) {
        Plan newPlan = Arrays.stream(Plan.values())
                .filter(p -> p.name().equalsIgnoreCase(request.plan().strip()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Plan desconocido: " + request.plan() + " (válidos: FREE, PRO, BUSINESS)"));

        UserAccount user = users.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta del token ya no existe"));
        user.changePlan(newPlan);
        return UserDto.from(users.save(user));
    }
}
