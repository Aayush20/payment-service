package org.example.paymentservice.services;

import org.example.paymentservice.clients.AuthClient;
import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final AuthClient authClient;

    public TokenService(AuthClient authClient) {
        this.authClient = authClient;
    }

    public TokenIntrospectionResponseDTO introspect(String tokenHeader) {
        return authClient.introspectToken(tokenHeader);
    }
}
