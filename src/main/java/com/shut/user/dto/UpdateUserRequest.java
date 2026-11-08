package com.shut.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 100) String telegramUsername,
        @Pattern(regexp = "^$|^[+0-9() -]{7,20}$", message = "شماره تلفن معتبر نیست")
        String phoneNumber
) {
}
