package org.folio.roles.mapper;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.PermissionMigrationJobs;
import org.folio.roles.domain.entity.PermissionMigrationJobEntity;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateConvertHelper.class)
public interface PermissionMigrationMapper {

  PermissionMigrationJob toDto(PermissionMigrationJobEntity entity);

  List<PermissionMigrationJob> toDtos(Iterable<PermissionMigrationJobEntity> entities);

  default PermissionMigrationJobs toDtoCollection(Page<PermissionMigrationJobEntity> pageable) {
    var dtos = emptyIfNull(toDtos(pageable));

    return new PermissionMigrationJobs()
      .migrations(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }
}
