package org.geoverity.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.entity.ApiClient;
import org.geoverity.service.ApiClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiClientService apiClientService;

    @Value("${geoverity.security.admin-api-key:gv_admin_master_secret_key_884920}")
    private String adminMasterKey;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Check Admin Master Key for /api/v1/admin/** endpoints
        String adminKey = request.getHeader(ADMIN_KEY_HEADER);
        if (adminKey == null) {
            adminKey = request.getHeader(API_KEY_HEADER); // Fallback to X-API-Key header
        }

        if (adminKey != null && adminKey.equals(adminMasterKey)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "admin", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check Client API Key for client endpoints
        String clientApiKey = request.getHeader(API_KEY_HEADER);
        if (clientApiKey != null && !clientApiKey.isBlank()) {
            Optional<ApiClient> clientOpt = apiClientService.authenticateApiKey(clientApiKey);
            if (clientOpt.isPresent()) {
                ApiClient client = clientOpt.get();
                request.setAttribute("authenticatedApiClient", client);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        client, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Invalid or revoked API key attempt from IP {}", request.getRemoteAddr());
            }
        }

        filterChain.doFilter(request, response);
    }
}
