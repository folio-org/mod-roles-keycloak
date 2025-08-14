package org.folio.roles.mapper.entity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.entity.EmbeddableEndpoint;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for mapping {@link Capability} objects to {@link CapabilityEntity} objects and vice versa.
 */
@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface CapabilityEntityMapper {

  @AuditableEntityMapping
  CapabilityEntity convert(Capability capability);

  EmbeddableEndpoint convert(Endpoint endpoint);

  @AuditableMapping
  Capability convert(CapabilityEntity entity);

  List<Capability> convert(List<CapabilityEntity> entities);

  @Mapping(target = "id", ignore = true)
  void update(Capability source, @MappingTarget Capability target);
}
