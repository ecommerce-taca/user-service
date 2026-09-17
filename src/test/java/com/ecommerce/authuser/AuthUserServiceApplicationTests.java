package com.ecommerce.authuser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import com.ecommerce.authuser.support.container.MySqlTestContainerConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(MySqlTestContainerConfiguration.class)
class AuthUserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
