package com.niletel.bsscore.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Legacy wire formats: date dd/MM/yyyy, timestamp dd/MM/yyyy HH:mm:ss (24 h, Cairo local time, no zone). */
public final class LegacyFormats {

    // "uuuu" and not "yyyy": year-of-era does not resolve under ResolverStyle.STRICT.
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private LegacyFormats() {
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? "" : TIMESTAMP.format(timestamp);
    }

    /** @throws BusinessException VAL001 when the text is null or not a real dd/MM/yyyy date */
    public static LocalDate parseDate(String text) {
        if (text == null) {
            throw new BusinessException(ErrorCode.VAL001);
        }
        try {
            return LocalDate.parse(text, DATE);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VAL001);
        }
    }
}
