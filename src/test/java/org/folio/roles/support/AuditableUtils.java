package org.folio.roles.support;

import static java.time.ZoneOffset.UTC;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.entity.Auditable;

@UtilityClass
public class AuditableUtils {

  public static void populateAuditable(Auditable entity, Metadata md) {
    if (md == null) {
      return;
    }
    entity.setCreatedBy(md.getCreatedBy());
    entity.setCreatedDate(dateAsOffsetDateTime(md.getCreatedDate()));
    entity.setUpdatedBy(md.getModifiedBy());
    entity.setUpdatedDate(dateAsOffsetDateTime(md.getModifiedDate()));
  }

  private static OffsetDateTime dateAsOffsetDateTime(Date date) {
    if (date == null) {
      return null;
    }
    return OffsetDateTime.from(date.toInstant().atOffset(UTC));
  }
}
