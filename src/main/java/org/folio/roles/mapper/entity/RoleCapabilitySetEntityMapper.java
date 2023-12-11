package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.RoleCapabilitySet;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface RoleCapabilitySetEntityMapper {

  @AuditableMapping
  RoleCapabilitySet convert(RoleCapabilitySetEntity entity);

  @AuditableEntityMapping
  RoleCapabilitySetEntity convert(RoleCapabilitySet dto);
}
