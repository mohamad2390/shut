package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;

/** How a member is named in a widget heading. */
final class WidgetNames {

    private static final String NO_NAME = "بدون نام";

    private WidgetNames() {
    }

    static String of(ProviderDetails provider) {
        String name = provider.profile().displayName();
        return name == null || name.isBlank() ? NO_NAME : name;
    }
}
