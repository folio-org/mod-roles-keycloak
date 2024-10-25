package org.folio.roles.domain.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CapabilityReplacements(
  Map<String, Set<String>> oldCapabilitiesToNewCapabilities,
  Map<String, Set<UUID>> oldCapabilityRoleAssignments,
  Map<String, Set<UUID>> oldCapabilityUserAssignments,
  Map<String, Set<UUID>> oldCapabilitySetRoleAssignments,
  Map<String, Set<UUID>> oldCapabilitySetUserAssignments) {
}
