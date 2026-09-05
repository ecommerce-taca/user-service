package com.ecommerce.authuser.favorite.exception;

public class InvalidFavoriteInputException extends RuntimeException {

    public InvalidFavoriteInputException() {
        super("Invalid favorite input");
    }
}