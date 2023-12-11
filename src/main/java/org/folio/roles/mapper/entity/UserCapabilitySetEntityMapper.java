package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.UserCapabilitySet;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface UserCapabilitySetEntityMapper {

  @AuditableMapping
  UserCapabilitySet convert(UserCapabilitySetEntity entity);

  @AuditableEntityMapping
  UserCapabilitySetEntity convert(UserCapabilitySet dto);
}
