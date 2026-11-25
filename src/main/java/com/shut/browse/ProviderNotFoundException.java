package com.shut.browse;

/** There is no active member with a public page under this id. */
public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(Long userId) {
        super("Provider not found: " + userId);
    }
}
