package com.shut.api.admin;

/** Switches an account or a venue on ({@code true}) or off ({@code false}). */
public record ActiveRequest(Boolean active) {
}
