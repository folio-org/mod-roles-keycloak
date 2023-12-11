package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface UserRoleMapper {

  @AuditableEntityMapping
  UserRoleEntity toEntity(UserRole rolesUser);

  List<UserRoleEntity> toEntity(List<UserRole> rolesUser);

  @AuditableMapping
  UserRole toDto(UserRoleEntity entity);

  List<UserRole> toDto(List<UserRoleEntity> rolesUserEntities);
}
