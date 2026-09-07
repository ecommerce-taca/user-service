package com.ecommerce.authuser.auth.jwks;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private static final String CACHE_CONTROL = "public, max-age=600";

    private final JwksService jwksService;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResult> getJwks() {

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        CACHE_CONTROL
                )
                .body(jwksService.get());
    }
}
