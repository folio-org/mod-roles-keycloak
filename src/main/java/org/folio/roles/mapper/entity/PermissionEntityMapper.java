package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.integration.kafka.model.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR)
public interface PermissionEntityMapper {

  PermissionEntity toEntity(Permission permission);
}
