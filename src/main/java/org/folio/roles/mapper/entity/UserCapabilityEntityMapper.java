package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.UserCapability;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface UserCapabilityEntityMapper {

  @AuditableMapping
  UserCapability convert(UserCapabilityEntity entity);

  @AuditableEntityMapping
  UserCapabilityEntity convert(UserCapability dto);
}
