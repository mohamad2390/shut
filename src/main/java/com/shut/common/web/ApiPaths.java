package com.shut.common.web;

/**
 * Central registry of all HTTP route paths, grouped by feature.
 * Fields of a nested interface are implicitly public static final,
 * so they can be used directly inside annotations such as @RequestMapping.
 */
public interface ApiPaths {

    String API_V1 = "/api/v1";

    /** Users module. */
    interface Users {
        String BASE = API_V1 + "/users";
        String ONBOARDING = "/onboarding";
        String BY_ID = "/{id}";
    }

    /** Professional profile module (nested under a user). */
    interface Profiles {
        String BASE = Users.BASE + "/{userId}/professional-profile";
    }

    /** Operational / documentation endpoints. */
    interface Ops {
        String HEALTH = "/actuator/health";
        String SWAGGER_UI = "/swagger-ui/index.html";
    }
}
