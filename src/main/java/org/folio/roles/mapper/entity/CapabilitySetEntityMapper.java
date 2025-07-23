package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;

/**
 * Mapper for mapping {@link Capability} objects to {@link CapabilityEntity} objects and vice versa.
 */
@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface CapabilitySetEntityMapper {

  @AuditableEntityMapping
  CapabilitySetEntity convert(CapabilitySet capability);

  @AuditableMapping
  CapabilitySet convert(CapabilitySetEntity entity);

  List<CapabilitySet> convert(List<CapabilitySetEntity> entities);

  List<CapabilitySetEntity> mapToEntities(List<CapabilitySet> capabilities);
}
