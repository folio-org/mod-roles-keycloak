package org.folio.roles.mapper;

import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.entity.PermissionMigrationJobEntity;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateConvertHelper.class)
public interface PermissionMigrationMapper {

  PermissionMigrationJob toDto(PermissionMigrationJobEntity entity);
}
