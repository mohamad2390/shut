package com.shut.telegram.profile;

import com.shut.common.text.PersianDigits;
import com.shut.profile.dto.ProfessionalProfileResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** The profile as text. Used both for a user's own profile and for the pages other members see. */
public final class ProfileCard {

    private ProfileCard() {
    }

    public static String text(ProfessionalProfileResponse profile, String fallbackName) {
        List<String> lines = new ArrayList<>();
        lines.add("👤 " + (profile.displayName() != null ? profile.displayName() : fallbackName));
        if (profile.headline() != null) {
            lines.add(profile.headline());
        }
        lines.add("");

        if (profile.yearsExperience() != null && profile.yearsExperience() > 0) {
            lines.add("سابقه: " + PersianDigits.toPersian(profile.yearsExperience()) + " سال");
        }
        addJoined(lines, "شهر", Arrays.asList(profile.baseCity(), profile.baseCountry()));
        addJoined(lines, "مهارت‌ها", profile.skills());
        addJoined(lines, "شهرهای فعالیت", profile.serviceCities());
        addJoined(lines, "همکاری‌ها", profile.collaborators());
        if (profile.equipmentDescription() != null) {
            lines.add("تجهیزات: " + profile.equipmentDescription());
        }
        if (profile.websiteUrl() != null) {
            lines.add("وب‌سایت: " + profile.websiteUrl());
        }
        if (profile.instagramUsername() != null) {
            lines.add("اینستاگرام: @" + profile.instagramUsername());
        }
        if (profile.bio() != null) {
            lines.add("");
            lines.add("درباره:");
            lines.add(profile.bio());
        }
        return String.join("\n", lines);
    }

    /** Adds "label: a، b، c" when at least one value is present. */
    private static void addJoined(List<String> lines, String label, Collection<String> values) {
        String joined = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("، "));
        if (!joined.isEmpty()) {
            lines.add(label + ": " + joined);
        }
    }
}
