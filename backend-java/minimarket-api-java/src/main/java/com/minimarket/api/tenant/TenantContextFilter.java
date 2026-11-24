package com.minimarket.api.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "miroo.tenancy", name = "enabled", havingValue = "true")
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(TenantResolver tenantResolver, ObjectMapper objectMapper) {
        this.tenantResolver = tenantResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        TenantRecord tenant = tenantResolver.resolve(request.getServerName());
        if (tenant == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(), Map.of("message", "Negocio no disponible."));
            return;
        }

        try {
            TenantContext.set(tenant);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
