package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface LoadableRoleEntityMapper {

  @AuditableEntityMapping
  LoadableRoleEntity toRoleEntity(LoadableRole role);

  @AuditableMapping
  LoadableRole toRole(LoadableRoleEntity entity);

  @AuditableEntityMapping
  @Mapping(target = "role", ignore = true)
  LoadablePermissionEntity toPermissionEntity(LoadablePermission role);

  @AuditableMapping
  LoadablePermission toPermission(LoadablePermissionEntity entity);
}
