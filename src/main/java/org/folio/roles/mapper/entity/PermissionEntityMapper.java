package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.Collection;
import java.util.List;
import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.integration.kafka.model.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR)
public interface PermissionEntityMapper {

  Permission toDto(PermissionEntity entity);

  List<Permission> toDto(Collection<PermissionEntity> entity);

  PermissionEntity toEntity(Permission permission);
}
