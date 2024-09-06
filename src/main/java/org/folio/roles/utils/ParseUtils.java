package org.folio.roles.utils;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParseUtils {

  private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Parses int if possible or return false if parsing failed or value is not integer.
   *
   * @param value - value toa analyze and parse
   */
  public static Integer parseIntSafe(String value) {
    if (value == null) {
      return null;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn("Failed to parse '{}' as integer value", value);
      return null;
    }
  }

  /**
   * Parses incoming string to date using default pattern: {@code yyyy-MM-dd HH:mm:ss}.
   *
   * @param dateString - incoming date string to pars
   * @return null if incoming string is null or blank or Date,
   * @see java.time.LocalDateTime#parse(CharSequence, DateTimeFormatter) for implementation details
   */
  public static Date parseDateSafe(String dateString) {
    if (StringUtils.isBlank(dateString)) {
      return null;
    }

    try {
      var localDateTime = LocalDateTime.parse(dateString, DEFAULT_DATE_TIME_FORMATTER);
      return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    } catch (DateTimeParseException dateTimeParseException) {
      log.warn("Failed to parse '{}' as date", dateString, dateTimeParseException);
      return null;
    }
  }
}
