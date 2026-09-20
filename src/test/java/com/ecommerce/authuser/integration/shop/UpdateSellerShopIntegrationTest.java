package com.ecommerce.authuser.integration.shop;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;
import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.repository.ShopRepository;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.support.builder.ShopTestBuilder;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.support.security.TestJwtFactory;
import com.ecommerce.authuser.support.security.TestUserToken;
import com.ecommerce.authuser.user.domain.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UpdateSellerShopIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private OutboxPayloadProtector outboxPayloadProtector;

    @Test
    void updateSellerShop_shouldCreateShopUpdatedOutboxEvent() throws Exception {
        User seller = UserTestBuilder
                .aUser()
                .withEmail("seller-shop-update@test.com")
                .withEmailNormalized("seller-shop-update@test.com")
                .withFullName("Seller User")
                .build();

        seller = userRepository.saveAndFlush(seller);

        Shop shop = ShopTestBuilder
                .forOwner(seller)
                .withName("Old Shop Name")
                .withSlug("seller-shop-update")
                .withBusinessName("Seller Shop Business")
                .withDescription("Old description")
                .build();

        shop = shopRepository.saveAndFlush(shop);

        Role sellerRole = roleRepository
                .findByRoleKey(RbacKeys.Roles.SELLER)
                .orElseThrow();

        UserRole sellerAssignment = UserRole.assign(
                seller,
                sellerRole,
                shop,
                seller.getId()
        );

        userRoleRepository.saveAndFlush(sellerAssignment);

        TestUserToken token = TestJwtFactory.createUserToken(
                seller.getId(),
                List.of(RbacKeys.Roles.SELLER)
        );

        mockMvc.perform(
                        put("/api/v1/seller/shop")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "name": "New Shop Name",
                                                    "description": "New description"
                                                }
                                                """
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name")
                        .value("New Shop Name"))
                .andExpect(jsonPath("$.data.description")
                        .value("New description"));

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.SHOP,
                        shop.getId()
                );

        OutboxEvent shopUpdatedEvent = events.stream()
                .filter(event -> "shop.updated".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(shopUpdatedEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.SHOP);

        assertThat(shopUpdatedEvent.getAggregateId())
                .isEqualTo(shop.getId());

        assertThat(shopUpdatedEvent.getActorUserId())
                .isEqualTo(seller.getId());

        assertThat(shopUpdatedEvent.getPartitionKey())
                .isEqualTo(shop.getId().toString());

        Map<String, Object> protectedPayload =
                shopUpdatedEvent.getPayloadView();

        assertThat(protectedPayload)
                .containsKeys(
                        "protected",
                        "alg",
                        "key_version",
                        "iv",
                        "ciphertext"
                );

        Map<String, Object> plainPayload =
                outboxPayloadProtector.unprotect(
                        "shop.updated",
                        protectedPayload
                );

        assertThat(plainPayload)
                .containsEntry(
                        "shop_id",
                        shop.getId().toString()
                );

        assertThat(plainPayload.get("changed_fields"))
                .isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> changedFields =
                (List<String>) plainPayload.get("changed_fields");

        assertThat(changedFields)
                .containsExactly(
                        "name",
                        "description"
                );

        assertThat(plainPayload.get("snapshot"))
                .isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot =
                (Map<String, Object>) plainPayload.get("snapshot");

        assertThat(snapshot)
                .containsEntry("name", "New Shop Name")
                .containsEntry("description", "New description");

        assertThat(snapshot)
                .doesNotContainKey("business_name");

        assertThat(plainPayload)
                .containsKey("updated_at");

        assertThat(String.valueOf(plainPayload.get("updated_at")))
                .isNotBlank();

        assertThat(plainPayload)
                .containsKey("version");

        assertThat(plainPayload.get("version"))
                .isInstanceOf(Number.class);
    }
}