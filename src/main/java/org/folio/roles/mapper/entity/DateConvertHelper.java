package org.folio.roles.mapper.entity;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;

import java.time.OffsetDateTime;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * Component for converting dates. Uses for mapstruct's mappers.
 */
@Component
public class DateConvertHelper {

  /**
   * Converts an {@link OffsetDateTime} to a {@link Date}.
   *
   * @param offsetDateTime the {@link OffsetDateTime} object to be converted.
   * @return the converted {@link Date} object, or {@code null} if the input {@code date} is {@code null}.
   */
  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    if (isNull(offsetDateTime)) {
      return null;
    }
    return Date.from(offsetDateTime.toInstant());
  }

  /**
   * Converts a {@link Date} object to an {@link OffsetDateTime} object.
   *
   * @param date the {@link Date} object to be converted.
   * @return the converted {@link OffsetDateTime} object, or {@code null} if the input {@code date} is {@code null}.
   */
  public OffsetDateTime dateAsOffsetDateTime(Date date) {
    if (isNull(date)) {
      return null;
    }
    return OffsetDateTime.from(date.toInstant().atOffset(UTC));
  }
}
