package com.torii.auth;

import com.torii.auth.AuthDtos.UserDto;
import com.torii.history.SavedSearchDto;
import com.torii.history.SearchHistoryService;
import com.torii.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Datos del usuario autenticado ("me" = el dueño del token). Todo lo que cuelga
 * de {@code /api/me/**} exige token válido (ver SecurityConfig); Spring inyecta el
 * JWT ya verificado como principal, y su subject es el id del usuario.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;
    private final SearchHistoryService searchHistory;

    public MeController(UserRepository users, SearchHistoryService searchHistory) {
        this.users = users;
        this.searchHistory = searchHistory;
    }

    /** Perfil del usuario del token (para restaurar la sesión al recargar la página). */
    @GetMapping
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta del token ya no existe"));
    }

    /** Las últimas búsquedas del usuario, para repetirlas con un clic. */
    @GetMapping("/searches")
    public List<SavedSearchDto> mySearches(@AuthenticationPrincipal Jwt jwt) {
        return searchHistory.recentSearches(Long.valueOf(jwt.getSubject()));
    }
}
