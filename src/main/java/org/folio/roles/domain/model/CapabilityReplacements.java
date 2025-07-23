package org.folio.roles.domain.model;

import java.util.HashMap;
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
  Map<String, Set<CapabilitySet>> oldCapabSetByDummyCapabilityPermission) {

  /**
   * Returns a map of old permissions to new permissions for replacement
   * excluding old as a key related to dummy capabilities.
   *
   * @return a map of old permissions as a key to new permissions
   */
  public Map<String, Set<String>> getReplacementsExcludeDummy() {
    var excludeDummy = new HashMap<>(oldPermissionsToNewPermissions);
    excludeDummy.keySet().removeAll(oldCapabSetByDummyCapabilityPermission.keySet());
    return excludeDummy;
  }

  /**
   * Returns a map of old permissions to new permissions for replacement
   * including only old as a key related to dummy capabilities.
   *
   * @return a map of old permissions as a key to new permissions
   */
  public Map<String, Set<String>> getReplacementsOnlyDummy() {
    var dummy = new HashMap<String, Set<String>>();
    oldPermissionsToNewPermissions
      .forEach((key, value) -> {
        if (oldCapabSetByDummyCapabilityPermission.containsKey(key)) {
          dummy.put(key, value);
        }
      });
    return dummy;
  }
}
