package com.ecommerce.authuser.shopfollow.exception;

public class ShopFollowLimitReachedException extends RuntimeException {

    public ShopFollowLimitReachedException() {
        super("Shop follow limit reached");
    }
}
