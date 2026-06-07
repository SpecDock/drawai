package com.drawai.domain.auth.gateway;

/**
 * Domain-level contract for JWT issuance and parsing.
 *
 * <p>Defined in the domain layer; implemented by the infrastructure layer
 * (e.g. {@code com.drawai.infrastructure.security.JwtUtil}). The trigger layer
 * (HTTP controllers / filters) depends on this interface only, never on any
 * concrete implementation, preserving the dependency rule
 * {@code trigger -> domain <- infrastructure}.</p>
 *
 * <p>All methods are required to be safe to call from request-handling threads.</p>
 * @author 29287
 */
public interface JwtService {

    /**
     * Issue a signed JWT for the given subject (typically a username).
     *
     * @param subject the principal to embed as the JWT {@code sub} claim; must not be {@code null}
     * @return a compact, signed JWT string
     */
    String issue(String subject);

    /**
     * Parse a JWT and return its subject claim.
     *
     * @param token the compact JWT string (without the {@code Bearer } prefix)
     * @return the {@code sub} claim of the token
     */
    String parseSubject(String token);
}
