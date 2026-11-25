package com.shut.auth;

/** Sends the one-time code to a phone. The SMS company is chosen by {@code app.sms.provider}. */
public interface SmsSender {

    /** @param phone normalized, {@code 09xxxxxxxxx} */
    void send(String phone, String code);
}
