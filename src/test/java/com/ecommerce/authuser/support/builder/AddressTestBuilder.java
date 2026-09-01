package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.support.testdata.AddressTestData;
import com.ecommerce.authuser.user.domain.User;

public final class AddressTestBuilder {

    private User user;

    private String recipient =
            AddressTestData.RECIPIENT;

    private String phone =
            AddressTestData.PHONE;

    private String line1 =
            AddressTestData.LINE1;

    private String line2 =
            AddressTestData.LINE2;

    private String ward =
            AddressTestData.WARD;

    private String district =
            AddressTestData.DISTRICT;

    private String province =
            AddressTestData.PROVINCE;

    private String postalCode =
            AddressTestData.POSTAL_CODE;

    private boolean defaultAddress =
            AddressTestData.DEFAULT_ADDRESS;

    private AddressTestBuilder() {
    }

    public static AddressTestBuilder anAddress() {
        return new AddressTestBuilder();
    }

    public AddressTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public AddressTestBuilder withRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public AddressTestBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public AddressTestBuilder withLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public AddressTestBuilder withLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public AddressTestBuilder withWard(String ward) {
        this.ward = ward;
        return this;
    }

    public AddressTestBuilder withDistrict(String district) {
        this.district = district;
        return this;
    }

    public AddressTestBuilder withProvince(String province) {
        this.province = province;
        return this;
    }

    public AddressTestBuilder withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public AddressTestBuilder asDefault() {
        this.defaultAddress = true;
        return this;
    }

    public AddressTestBuilder asNonDefault() {
        this.defaultAddress = false;
        return this;
    }

    public Address build() {
        if (user == null) {
            throw new IllegalStateException(
                    "user must be provided"
            );
        }

        return Address.create(
                user,
                recipient,
                phone,
                line1,
                line2,
                ward,
                district,
                province,
                postalCode,
                defaultAddress
        );
    }
}
