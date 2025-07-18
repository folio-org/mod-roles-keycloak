package org.folio.roles.domain.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.dto.CapabilitySet;

public record CapabilityReplacements(
  Map<String, Set<String>> oldPermissionsToNewPermissions,
  Map<String, Set<UUID>> oldRoleCapabByPermission,
  Map<String, Set<UUID>> oldUserCapabByPermission,
  Map<String, Set<UUID>> oldRoleCapabSetByPermission,
  Map<String, Set<UUID>> oldUserCapabSetByPermission,
  Map<String, Set<CapabilitySet>> oldCapabSetDummyCapabByPermission) {
}
