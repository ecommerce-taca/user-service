package com.ecommerce.authuser.user.application.profile;

import com.ecommerce.authuser.auth.application.IdentityNormalizer;
import com.ecommerce.authuser.auth.exception.InvalidPhoneFormatException;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.ProfileInvalidException;
import com.ecommerce.authuser.user.exception.profile.ProfilePhoneAlreadyExistsException;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UpdateMyProfileService {

    private final UserRepository userRepository;

    private final IdentityNormalizer identityNormalizer;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    private final GetMyProfileService getMyProfileService;

    @Transactional
    public UpdateMyProfileResult update(UpdateMyProfileCommand command) {
        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        String fullName = normalizeFullName(command.fullName());

        String normalizedPhone = resolvePhone(command);

        validateDateOfBirth(command);

        boolean fullNameChanged =
                !Objects.equals(user.getFullName(), fullName);

        boolean phoneChanged =
                command.phoneProvided()
                        && !Objects.equals(
                        user.getPhoneNormalized(),
                        normalizedPhone
                );

        boolean dateOfBirthChanged =
                command.dateOfBirthProvided()
                        && !Objects.equals(
                        user.getDateOfBirth(),
                        command.dateOfBirth()
                );

        if (phoneChanged
                && normalizedPhone != null
                && userRepository.existsByPhoneNormalized(normalizedPhone)
        ) {
            throw new ProfilePhoneAlreadyExistsException();
        }

        List<String> changedFields = new ArrayList<>();

        if (fullNameChanged) {
            changedFields.add("full_name");
        }

        if (phoneChanged) {
            changedFields.add("phone");
        }

        if (dateOfBirthChanged) {
            changedFields.add("date_of_birth");
        }

        boolean changed = !changedFields.isEmpty();

        if (changed) {

            user.updateProfile(
                    fullName,
                    command.phoneProvided(),
                    normalizedPhone,
                    normalizedPhone,
                    command.dateOfBirthProvided(),
                    command.dateOfBirth()
            );

            try {
                userRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                if (phoneChanged && normalizedPhone != null) {
                    throw new ProfilePhoneAlreadyExistsException();
                }

                throw ex;
            }

            OutboxEvent event =
                    OutboxEvent.create(
                            OutboxAggregateType.USER,
                            user.getId(),
                            "user.updated",
                            (short) 1,
                            user.getId().toString(),

                            outboxPayloadProtector.protect(
                                    "user.updated",
                                    Map.of(
                                            "user_id",
                                            user.getId().toString(),

                                            "changed_fields",
                                            List.copyOf(
                                                    changedFields
                                            )
                                    )
                            )
                    );

            outboxEventRepository.save(event);
        }

        boolean phoneVerificationRequired =
                phoneChanged && normalizedPhone != null;

        GetMyProfileResult profile = getMyProfileService.get(user.getId());

        return new UpdateMyProfileResult(
                profile,
                phoneVerificationRequired
        );
    }

    private String normalizeFullName(String value) {
        if (value == null) {
            throw new ProfileInvalidException();
        }

        String normalized = value.strip();

        int characterCount =
                normalized.codePointCount(0, normalized.length());

        if (characterCount < 1 || characterCount > 120) {
            throw new ProfileInvalidException();
        }

        return normalized;
    }

    private String resolvePhone(UpdateMyProfileCommand command) {
        if (!command.phoneProvided()) {
            return null;
        }

        String phone = command.phone();

        if (phone == null) {
            return null;
        }

        if (phone.isBlank()) {
            throw new ProfileInvalidException();
        }

        try {
            return identityNormalizer.normalizePhone(phone);
        } catch (InvalidPhoneFormatException ex) {
            throw new ProfileInvalidException();
        }
    }

    private void validateDateOfBirth(UpdateMyProfileCommand command) {
        if (!command.dateOfBirthProvided()) {
            return;
        }

        LocalDate dateOfBirth = command.dateOfBirth();

        if (dateOfBirth == null) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (dateOfBirth.isAfter(today)) {
            throw new ProfileInvalidException();
        }
    }
}
