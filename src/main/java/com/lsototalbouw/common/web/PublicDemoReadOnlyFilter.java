package com.lsototalbouw.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects the public portfolio demo from data changes.
 *
 * <p>When the public demo flag is enabled, authenticated users can navigate the application and inspect
 * the prepared fictional dataset, but state-changing requests are rejected before they reach controllers.
 */
@Component
public class PublicDemoReadOnlyFilter extends OncePerRequestFilter {

    private static final Set<String> READ_ONLY_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name()
    );

    private final boolean publicDemoEnabled;

    public PublicDemoReadOnlyFilter(@Value("${app.public-demo.enabled:false}") boolean publicDemoEnabled) {
        this.publicDemoEnabled = publicDemoEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isBlockedDemoWrite(request)) {
            response.sendRedirect(safeBackUrl(request));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isBlockedDemoWrite(HttpServletRequest request) {
        if (!publicDemoEnabled || READ_ONLY_METHODS.contains(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return !"/login".equals(path) && !"/logout".equals(path);
    }

    private String safeBackUrl(HttpServletRequest request) {
        return request.getContextPath() + "/dashboard?demoReadOnly";
    }
}
