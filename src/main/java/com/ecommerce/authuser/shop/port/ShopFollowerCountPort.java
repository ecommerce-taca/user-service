package com.ecommerce.authuser.shop.port;

import java.util.UUID;

public interface ShopFollowerCountPort {

    long countFollowers(UUID shopId);
}
