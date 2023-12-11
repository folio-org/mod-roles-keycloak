package org.folio.roles.mapper.entity;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import java.util.Objects;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

/**
 * Mapper for mapping {@link Role} objects to {@link RoleEntity} objects and vice versa.
 */
@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface RoleEntityMapper {

  @AuditableEntityMapping
  RoleEntity toRoleEntity(Role role);

  /**
   * Maps a {@link RoleEntity} object to a {@link Role} object.
   *
   * @param entity the {@link RoleEntity} object to be mapped.
   * @return the mapped {@link Role} object.
   */
  @AuditableMapping
  Role toRole(RoleEntity entity);

  /**
   * Maps a list of {@link RoleEntity} objects to a list of {@link Role} objects.
   *
   * @param entities the list of {@link RoleEntity} objects to be mapped.
   * @return the mapped list of {@link Role} objects.
   */
  default List<Role> toRole(List<RoleEntity> entities) {
    if (isEmpty(entities)) {
      return List.of();
    }
    return entities.stream().map(this::toRole).filter(Objects::nonNull).collect(toList());
  }
}
