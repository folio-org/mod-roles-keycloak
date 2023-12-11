package org.folio.roles.integration.kafka.model;

import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;

public record ResultHolder(Capability capability, CapabilitySet capabilitySet) {}
