package org.folio.roles.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.folio.roles.domain.dto.CapabilitySet;
import org.junit.jupiter.api.Test;

class CapabilityReplacementsTest {

  @Test
  public void getReplacementsExcludeDummy_positive() {
    Map<String, Set<String>> oldPermissionsToNewPermissions = new HashMap<>();
    oldPermissionsToNewPermissions.put("perm1", Set.of("newPerm1"));
    oldPermissionsToNewPermissions.put("perm2", Set.of("newPerm2"));
    oldPermissionsToNewPermissions.put("dummyPerm", Set.of("dummyNewPerm"));

    Map<String, Set<CapabilitySet>> oldCapabSetByDummyCapabilityPermission = new HashMap<>();
    oldCapabSetByDummyCapabilityPermission.put("dummyPerm", Set.of());

    var replacements = new CapabilityReplacements(
      oldPermissionsToNewPermissions,
      Map.of(),
      Map.of(),
      Map.of(),
      Map.of(),
      oldCapabSetByDummyCapabilityPermission
    );

    var result = replacements.getReplacementsExcludeDummy();

    assertEquals(2, result.size());
    assertTrue(result.containsKey("perm1"));
    assertTrue(result.containsKey("perm2"));
    assertFalse(result.containsKey("dummyPerm"));
  }

  @Test
  void getReplacementsOnlyDummy_positive() {
    Map<String, Set<String>> oldPermissionsToNewPermissions = new HashMap<>();
    oldPermissionsToNewPermissions.put("perm1", Set.of("newPerm1"));
    oldPermissionsToNewPermissions.put("perm2", Set.of("newPerm2"));
    oldPermissionsToNewPermissions.put("dummyPerm", Set.of("dummyNewPerm"));

    Map<String, Set<CapabilitySet>> oldCapabSetByDummyCapabilityPermission = new HashMap<>();
    oldCapabSetByDummyCapabilityPermission.put("dummyPerm", Set.of());

    var replacements = new CapabilityReplacements(
      oldPermissionsToNewPermissions,
      Map.of(),
      Map.of(),
      Map.of(),
      Map.of(),
      oldCapabSetByDummyCapabilityPermission
    );

    var result = replacements.getReplacementsOnlyDummy();

    assertEquals(1, result.size());
    assertTrue(result.containsKey("dummyPerm"));
  }
}
