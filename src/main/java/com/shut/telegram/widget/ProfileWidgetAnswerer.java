package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.equipment.EquipmentService;
import com.shut.review.ReviewService;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** The profile widget only asks for its state when Telegram did not hand it one with the message. */
@Component
@TelegramBotEnabled
class ProfileWidgetAnswerer implements WidgetAnswerer {

    private final ReviewService reviews;
    private final EquipmentService equipment;

    ProfileWidgetAnswerer(ReviewService reviews, EquipmentService equipment) {
        this.reviews = reviews;
        this.equipment = equipment;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.PROFILE;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return ProfileWidgetState.of(provider, reviews.summary(provider.userId()), equipment.lines(provider.userId()));
    }
}
