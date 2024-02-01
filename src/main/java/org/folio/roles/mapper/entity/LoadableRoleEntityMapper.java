package org.folio.roles.mapper.entity;

import static java.util.stream.Collectors.toList;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import java.util.Objects;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface LoadableRoleEntityMapper {

  @AuditableEntityMapping
  LoadableRoleEntity toRoleEntity(LoadableRole role);

  @AuditableMapping
  LoadableRole toRole(LoadableRoleEntity entity);

  default List<LoadableRole> toRoles(List<LoadableRoleEntity> entities) {
    return toStream(entities).map(this::toRole).filter(Objects::nonNull).collect(toList());
  }
}
