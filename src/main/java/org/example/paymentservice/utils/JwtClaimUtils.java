package org.example.paymentservice.utils;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

public class JwtClaimUtils {
    public static boolean hasRole(Jwt jwt, String requiredRole) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(requiredRole);
    }

    public static boolean hasScope(Jwt jwt, String requiredScope) {
        List<String> scopes = jwt.getClaimAsStringList("scopes");
        return scopes != null && scopes.contains(requiredScope);
    }

    public static boolean isSystemCall(Jwt jwt, String expectedSub) {
        return expectedSub.equalsIgnoreCase(jwt.getSubject());
    }
}
