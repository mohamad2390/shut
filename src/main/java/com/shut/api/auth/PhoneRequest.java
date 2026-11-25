package com.shut.api.auth;

/** The phone number to send a code to; Persian digits and {@code +98} are fine. */
public record PhoneRequest(String phone) {
}
