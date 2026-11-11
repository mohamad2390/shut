package com.shut.telegram.project;

import com.shut.common.calendar.JalaliDate;
import com.shut.common.calendar.JalaliMonth;
import com.shut.common.text.PersianDigits;
import com.shut.project.BidStatus;
import com.shut.project.Project;
import com.shut.project.ProjectStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Pieces of text about projects and offers that several screens share. */
final class ProjectText {

    private ProjectText() {
    }

    static String money(long tomans) {
        return PersianDigits.toPersianGrouped(tomans) + " تومان";
    }

    /** "۲۱ آبان ۱۴۰۵، ساعت ۱۴:۳۰" in the given zone. */
    static String deadline(OffsetDateTime time, ZoneId zone) {
        var local = time.atZoneSameInstant(zone);
        JalaliDate date = JalaliDate.of(local.toLocalDate());
        return PersianDigits.toPersian(date.day()) + " " + new JalaliMonth(date.year(), date.month()).name() + " "
                + PersianDigits.toPersian(date.year()) + "، ساعت "
                + PersianDigits.toPersian(String.format("%02d:%02d", local.getHour(), local.getMinute()));
    }

    static String budget(Project project) {
        return project.getBudget() == null ? "اعلام نشده" : money(project.getBudget());
    }

    static String status(Project project, boolean takingBids) {
        if (project.getStatus() == ProjectStatus.AWARDED) {
            return "✅ واگذار شد";
        }
        if (project.getStatus() == ProjectStatus.CANCELLED) {
            return "🚫 لغو شد";
        }
        return takingBids ? "🟢 در حال دریافت پیشنهاد" : "⏳ مهلت تمام شد، منتظر انتخاب صاحب پروژه";
    }

    static String bidStatus(BidStatus status) {
        return switch (status) {
            case PENDING -> "⏳ در انتظار";
            case ACCEPTED -> "✅ انتخاب شد";
            case REJECTED -> "❌ انتخاب نشد";
        };
    }

    /** Short text for a button in a list, never longer than Telegram buttons look good at. */
    static String listLabel(Project project, String prefix) {
        String text = prefix + project.getTitle() + " — " + project.getCategory().label();
        return text.length() > 55 ? text.substring(0, 54) + "…" : text;
    }

    static String details(Project project, String ownerName, String deadline, boolean takingBids) {
        StringBuilder text = new StringBuilder("📋 " + project.getTitle() + "\n" + status(project, takingBids) + "\n\n");
        text.append("دسته: ").append(project.getCategory().label()).append("\n");
        text.append("شهر: ").append(project.getCity() == null ? "فرقی نمی‌کنه" : project.getCity()).append("\n");
        text.append("بودجه: ").append(budget(project)).append("\n");
        text.append("مهلت پیشنهاد: ").append(deadline).append("\n");
        text.append("صاحب پروژه: ").append(ownerName).append("\n\n");
        text.append(project.getDescription());
        return text.toString();
    }
}
