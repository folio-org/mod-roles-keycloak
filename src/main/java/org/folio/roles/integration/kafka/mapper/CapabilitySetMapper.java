package org.folio.roles.integration.kafka.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.utils.CapabilityUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
  componentModel = "spring",
  injectionStrategy = CONSTRUCTOR,
  imports = CapabilityUtils.class)
public interface CapabilitySetMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "capabilities", ignore = true)
  @Mapping(target = "applicationId", source = "applicationId")
  @Mapping(target = "name", expression = "java(CapabilityUtils.getCapabilityName(csd.getResource(), csd.getAction()))")
  CapabilitySet convert(String applicationId, CapabilitySetDescriptor csd);
}
