package org.example.paymentservice.configs;

import org.example.paymentservice.utils.JwtClaimUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtUtilsConfig {
    @Bean
    public JwtClaimUtils jwtClaimUtils() {
        return new JwtClaimUtils();
    }
}
