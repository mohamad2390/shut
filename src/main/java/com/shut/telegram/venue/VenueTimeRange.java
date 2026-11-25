package com.shut.telegram.venue;

import com.shut.common.text.PersianDigits;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hours a member typed for a booking, such as "۹ تا ۱۲", "9-12" or "14:30 الی 18". */
record VenueTimeRange(LocalTime start, LocalTime end) {

    private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?:[:.](\\d{2}))?");

    /** Empty unless the text holds exactly two times, the second later than the first. 24 means the end of the day. */
    static Optional<VenueTimeRange> parse(String raw) {
        List<LocalTime> times = new ArrayList<>();
        Matcher matcher = TIME.matcher(PersianDigits.toAscii(raw).replace('٫', '.'));
        while (matcher.find()) {
            Optional<LocalTime> time = time(matcher.group(1), matcher.group(2));
            if (time.isEmpty()) {
                return Optional.empty();
            }
            times.add(time.get());
        }
        if (times.size() != 2 || !times.get(0).isBefore(times.get(1))) {
            return Optional.empty();
        }
        return Optional.of(new VenueTimeRange(times.get(0), times.get(1)));
    }

    private static Optional<LocalTime> time(String hour, String minute) {
        int h = Integer.parseInt(hour);
        int m = minute == null ? 0 : Integer.parseInt(minute);
        if (m > 59 || h > 24 || (h == 24 && m > 0)) {
            return Optional.empty();
        }
        return Optional.of(h == 24 ? LocalTime.of(23, 59) : LocalTime.of(h, m));
    }
}
