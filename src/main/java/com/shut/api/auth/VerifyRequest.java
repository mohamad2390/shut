package com.shut.api.auth;

public record VerifyRequest(String phone, String code) {
}
