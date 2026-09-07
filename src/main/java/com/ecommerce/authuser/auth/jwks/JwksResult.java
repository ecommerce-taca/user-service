package com.ecommerce.authuser.auth.jwks;

import java.util.List;

public record JwksResult(
        List<Key> keys
) {

    public JwksResult {
        keys = List.copyOf(keys);
    }

    public record Key(
            String kty,
            String kid,
            String use,
            String alg,
            String n,
            String e
    ) {
    }
}
