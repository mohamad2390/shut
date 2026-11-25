package com.shut.api.admin;

/** A new or changed service. When changing, a field that is not sent stays as it is. */
public record ServiceRequest(String name, String icon, Integer sortOrder, Boolean active, Boolean asksName) {
}
