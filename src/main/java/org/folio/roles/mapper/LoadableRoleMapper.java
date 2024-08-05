package org.folio.roles.mapper;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface LoadableRoleMapper {

  @AuditableEntityMapping
  LoadableRoleEntity toRoleEntity(LoadableRole role);

  List<LoadableRoleEntity> toRoleEntity(List<LoadableRole> role);

  @AuditableMapping
  LoadableRole toRole(LoadableRoleEntity entity);

  @AuditableMapping
  Role toRegularRole(LoadableRoleEntity entity);

  @AuditableEntityMapping
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "role", ignore = true)
  LoadablePermissionEntity toPermissionEntity(LoadablePermission role);

  List<LoadablePermissionEntity> toPermissionEntity(List<LoadablePermission> entity);

  @AuditableMapping
  LoadablePermission toPermission(LoadablePermissionEntity entity);

  List<LoadablePermission> toPermission(List<LoadablePermissionEntity> entity);

  default LoadableRoles toLoadableRoles(Page<LoadableRole> pageable) {
    return new LoadableRoles()
      .loadableRoles(emptyIfNull(pageable.getContent()))
      .totalRecords(pageable.getTotalElements());
  }
}
