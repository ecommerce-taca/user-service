package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.support.testdata.UserTestData;
import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class UserFixture {
    private final UserRepository userRepository;

    @Autowired
    public UserFixture(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(
            User user) {
        return userRepository.saveAndFlush(user);
    }

    public User createDefaultUser() {
        return create(
                UserTestData.defaultUser());
    }

    public User createBuyer() {
        return create(
                UserTestData.buyer());
    }

    public User createUser(
            String email,
            String fullName) {
        return create(
                UserTestData.user(
                        email,
                        fullName));
    }

    public User createUserWithPhone(
            String email,
            String phone,
            String fullName) {
        return create(
                UserTestData.userWithPhone(
                        email,
                        phone,
                        fullName));
    }

    public User createUserWithDateOfBirth(
            String email,
            String fullName,
            LocalDate dateOfBirth) {
        return create(
                UserTestData.userWithDateOfBirth(
                        email,
                        fullName,
                        dateOfBirth));
    }

    public User createUserWithPasswordHash(
            String email,
            String fullName,
            String passwordHash) {
        return create(
                UserTestData.userWithPasswordHash(
                        email,
                        fullName,
                        passwordHash));
    }

    public User create(
            UserTestBuilder builder) {
        return create(
                builder.build());
    }
}
