package org.folio.roles.mapper;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import org.folio.roles.domain.dto.PermissionMigrationError;
import org.folio.roles.domain.dto.PermissionMigrationErrors;
import org.folio.roles.domain.entity.PermissionMigrationErrorEntity;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface PermissionMigrationErrorMapper {

  /**
   * Converts {@link PermissionMigrationErrorEntity} to {@link PermissionMigrationError}.
   *
   * @param entity - permission migration error entity
   * @return permission migration error DTO
   */
  PermissionMigrationError toDto(PermissionMigrationErrorEntity entity);

  /**
   * Converts collection of {@link PermissionMigrationErrorEntity} to collection of {@link PermissionMigrationError}.
   *
   * @param entities - permission migration error entities
   * @return list of permission migration error DTOs
   */
  List<PermissionMigrationError> toDtos(Iterable<PermissionMigrationErrorEntity> entities);

  /**
   * Converts {@link Page} of {@link PermissionMigrationErrorEntity} to {@link PermissionMigrationErrors}.
   *
   * @param pageable - page of permission migration error entities
   * @return permission migration errors DTO
   */
  default PermissionMigrationErrors toDtoCollection(Page<PermissionMigrationErrorEntity> pageable) {
    var dtos = toDtos(pageable);
    return new PermissionMigrationErrors()
      .errors(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }

  /**
   * Maps {@link OffsetDateTime} to {@link Date}.
   *
   * @param offsetDateTime - offset date time
   * @return date
   */
  default Date map(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }
}
