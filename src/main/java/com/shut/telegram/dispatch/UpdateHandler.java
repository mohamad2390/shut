package com.shut.telegram.dispatch;

import com.shut.telegram.model.Update;

/** Handles one kind of incoming update. Add a new bean to support a new feature. */
public interface UpdateHandler {

    boolean supports(Update update);

    void handle(Update update);
}
