package org.folio.roles.integration.kafka.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import org.folio.common.utils.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.utils.CapabilityUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
  componentModel = "spring",
  injectionStrategy = CONSTRUCTOR,
  imports = {CapabilityUtils.class, CollectionUtils.class})
public interface CapabilitySetMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "capabilities", ignore = true)
  CapabilitySet convert(CapabilitySetDescriptor csd);

  @Mapping(target = "capabilities", expression = "java(CollectionUtils.mapItems(capabilityList, Capability::getId))")
  ExtendedCapabilitySet toExtendedCapabilitySet(CapabilitySet capabilitySet, List<Capability> capabilityList);
}
