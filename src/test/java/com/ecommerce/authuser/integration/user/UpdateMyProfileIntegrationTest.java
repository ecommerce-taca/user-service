package com.ecommerce.authuser.integration.user;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.support.security.TestJwtFactory;
import com.ecommerce.authuser.support.security.TestUserToken;
import com.ecommerce.authuser.user.domain.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UpdateMyProfileIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPayloadProtector outboxPayloadProtector;

    @Test
    void updateMyProfile_shouldCreateUserUpdatedOutboxPayloadContract() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("profile-update@test.com")
                .withEmailNormalized("profile-update@test.com")
                .withFullName("Old Name")
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "full_name": "New Name"
                                                }
                                                """
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name")
                        .value("New Name"));

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        user.getId()
                );

        OutboxEvent userUpdatedEvent = events.stream()
                .filter(event -> "user.updated".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(userUpdatedEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.USER);

        assertThat(userUpdatedEvent.getAggregateId())
                .isEqualTo(user.getId());

        assertThat(userUpdatedEvent.getActorUserId())
                .isEqualTo(user.getId());

        assertThat(userUpdatedEvent.getPartitionKey())
                .isEqualTo(user.getId().toString());

        Map<String, Object> protectedPayload =
                userUpdatedEvent.getPayloadView();

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
                        "user.updated",
                        protectedPayload
                );

        assertThat(plainPayload)
                .containsEntry(
                        "user_id",
                        user.getId().toString()
                );

        assertThat(plainPayload)
                .containsEntry(
                        "changed_fields",
                        List.of("full_name")
                );

        assertThat(plainPayload)
                .containsKey("updated_at");

        assertThat(String.valueOf(plainPayload.get("updated_at")))
                .isNotBlank();
    }

    @Test
    void updateMyProfile_shouldRejectShortVietnamesePhone() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("profile-phone-invalid@test.com")
                .withEmailNormalized("profile-phone-invalid@test.com")
                .withFullName("Old Name")
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "full_name": "New Name",
                                                    "phone": "+8434439845"
                                                }
                                                """
                                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("PROFILE_INVALID"));
    }

    @Test
    void updateMyProfile_shouldAllowNonVietnameseE164Phone() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("profile-phone-international@test.com")
                .withEmailNormalized("profile-phone-international@test.com")
                .withFullName("Old Name")
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "full_name": "New Name",
                                                    "phone": "+14155552671"
                                                }
                                                """
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone")
                        .value("+14155552671"));
    }

    @Test
    void updateMyProfile_shouldRejectDateOfBirthWhenUserIsYoungerThanFourteen() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("profile-underage@test.com")
                .withEmailNormalized("profile-underage@test.com")
                .withFullName("Old Name")
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        LocalDate dateOfBirth =
                LocalDate.now(ZoneOffset.UTC)
                        .minusYears(14)
                        .plusDays(1);

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "full_name": "New Name",
                                                    "date_of_birth": "%s"
                                                }
                                                """.formatted(dateOfBirth)
                                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("PROFILE_INVALID"));
    }

    @Test
    void updateMyProfile_shouldAllowDateOfBirthWhenUserIsExactlyFourteen() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("profile-fourteen@test.com")
                .withEmailNormalized("profile-fourteen@test.com")
                .withFullName("Old Name")
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        LocalDate dateOfBirth =
                LocalDate.now(ZoneOffset.UTC)
                        .minusYears(14);

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "full_name": "New Name",
                                                    "date_of_birth": "%s"
                                                }
                                                """.formatted(dateOfBirth)
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date_of_birth")
                        .value(dateOfBirth.toString()));
    }
}
