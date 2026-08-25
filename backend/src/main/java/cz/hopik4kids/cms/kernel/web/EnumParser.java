package cz.hopik4kids.cms.kernel.web;

import java.util.Locale;

/** Parses request enum strings into Java enums, raising a consistent ApiException on failure. */
public final class EnumParser {

    private EnumParser() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_" + field.toUpperCase(Locale.ROOT),
                    "Neplatná hodnota pole " + field + ": " + value);
        }
    }

    public static <E extends Enum<E>> E parseRequired(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("MISSING_" + field.toUpperCase(Locale.ROOT),
                    "Pole " + field + " je povinné");
        }
        return parse(type, value, field);
    }
}
