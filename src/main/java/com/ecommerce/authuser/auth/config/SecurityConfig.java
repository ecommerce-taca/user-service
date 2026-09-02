package com.ecommerce.authuser.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/signup",
                                        "/api/v1/auth/signin",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/email/verify",
                                        "/api/v1/auth/password/forgot",
                                        "/api/v1/auth/password/reset",
                                        "/api/v1/auth/2fa/verify"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )

                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(Customizer.withDefaults())
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
