package com.vault.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Allow all requests to the H2 console
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Disable CSRF protection only for the H2 console
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                // Allow frames so the H2 console UI can load properly
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}