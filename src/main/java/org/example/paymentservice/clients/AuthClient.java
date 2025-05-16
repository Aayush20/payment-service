package org.example.paymentservice.clients;

import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @PostMapping("/auth/validate")
    TokenIntrospectionResponseDTO introspectToken(@RequestHeader("Authorization") String token);
}
