package com.ecommerce.authuser.favorite.exception;

public class FavoriteLimitReachedException extends RuntimeException {

    public FavoriteLimitReachedException() {
        super("Favorite limit reached");
    }
}
