package org.folio.roles.domain.model;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
    return oldPermissionsToNewPermissions
      .entrySet()
      .stream()
      .filter(entry -> oldCapabSetByDummyCapabilityPermission.containsKey(entry.getKey()))
      .collect(toMap(Entry::getKey, Entry::getValue));
  }
}
