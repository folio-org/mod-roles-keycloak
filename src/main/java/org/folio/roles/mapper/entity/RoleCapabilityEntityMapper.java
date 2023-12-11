package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface RoleCapabilityEntityMapper {

  @AuditableMapping
  RoleCapability convert(RoleCapabilityEntity entity);

  @AuditableEntityMapping
  RoleCapabilityEntity convert(RoleCapability dto);
}
