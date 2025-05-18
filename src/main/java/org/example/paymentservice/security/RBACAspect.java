package org.example.paymentservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.example.paymentservice.services.TokenService;
import org.example.paymentservice.utils.TokenClaimUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RBACAspect {

    @Autowired
    private TokenService tokenService;

    @Before("@annotation(hasScope)")
    public void checkScope(JoinPoint joinPoint, HasScope hasScope) {
        String requiredScope = hasScope.value();
        String token = extractAuthHeader();
        TokenIntrospectionResponseDTO introspected = tokenService.introspect(token);
        if (!TokenClaimUtils.hasScope(introspected, requiredScope)) {
            throw new SecurityException("Access denied: missing required scope " + requiredScope);
        }
    }

    @Before("@annotation(AdminOnly)")
    public void checkAdminRole(JoinPoint joinPoint) {
        String token = extractAuthHeader();
        TokenIntrospectionResponseDTO introspected = tokenService.introspect(token);
        if (!TokenClaimUtils.hasRole(introspected, "ADMIN")) {
            throw new SecurityException("Access denied: admin role required");
        }
    }

    private String extractAuthHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new RuntimeException("Cannot access request context");
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader("Authorization");
    }
}
