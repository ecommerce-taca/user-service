package com.ecommerce.authuser.support.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @BeforeEach
    void setUpBaseTest() {
        // Common setup for integration tests.
        // Keep this class lightweight; individual fixtures belong to Fixture classes.
    }
}
