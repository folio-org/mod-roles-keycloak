package org.folio.roles.integration.kafka.model;

import java.util.List;
import org.folio.roles.domain.dto.Capability;

public record CapabilityResultHolder(List<Capability> capabilities, List<CapabilitySetDescriptor> capabilitySets) {}
