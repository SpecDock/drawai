package com.drawai.trigger.security;

import com.drawai.domain.auth.gateway.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login");

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean ignoreCase = "OPTIONS".equalsIgnoreCase(request.getMethod());
        log.info("是否为预检请求: {}", ignoreCase);
        return ignoreCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        log.info(path);
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            unauthorized(res, "missing bearer token");
            log.warn("拦截");
            return;
        }
        try {
            String subject = jwtService.parseSubject(auth.substring("Bearer ".length()));
            req.setAttribute("user", subject);
            log.info("放行");
            chain.doFilter(req, res);
        } catch (Exception e) {
            unauthorized(res, "invalid token");
        }
    }

    private void unauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
