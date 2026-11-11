package com.shut.telegram.project;

import com.shut.project.Project;
import com.shut.project.ProjectStatus;
import com.shut.showcase.ShowcaseCategory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTextTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 11, 8, 0, 0, 0, ZoneOffset.UTC);

    private static Project project(Long budget) {
        return new Project(1L, "عکاسی", ShowcaseCategory.PHOTOGRAPHY, "توضیح", null, budget, NOW, NOW.plusDays(7));
    }

    @Test
    void deadlineIsShownInJalaliInTheGivenZone() {
        // 18 Nov 2026 08:00 UTC is 11:30 in Tehran, 27 Aban 1405
        assertThat(ProjectText.deadline(NOW.plusDays(7), ZoneId.of("Asia/Tehran"))).isEqualTo("۲۷ آبان ۱۴۰۵، ساعت ۱۱:۳۰");
    }

    @Test
    void moneyUsesPersianDigitsAndGrouping() {
        assertThat(ProjectText.money(12_000_000)).isEqualTo("۱۲٬۰۰۰٬۰۰۰ تومان");
    }

    @Test
    void anUnstatedBudgetIsSaidSo() {
        assertThat(ProjectText.budget(project(null))).isEqualTo("اعلام نشده");
    }

    @Test
    void statusDependsOnTheDeadlineOnlyWhileOpen() {
        Project open = project(null);
        assertThat(ProjectText.status(open, true)).contains("🟢");
        assertThat(ProjectText.status(open, false)).contains("⏳");

        ReflectionTestUtils.setField(open, "status", ProjectStatus.AWARDED);
        assertThat(ProjectText.status(open, true)).contains("✅");
        ReflectionTestUtils.setField(open, "status", ProjectStatus.CANCELLED);
        assertThat(ProjectText.status(open, true)).contains("🚫");
    }

    @Test
    void listLabelsAreShortened() {
        Project longTitle = new Project(1L, "x".repeat(100), ShowcaseCategory.OTHER, "d", null, null, NOW, NOW.plusDays(1));

        assertThat(ProjectText.listLabel(longTitle, "").length()).isLessThanOrEqualTo(55);
    }
}
