package org.folio.roles.support;

import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.RoleUtils.ROLE_ID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.RoleCapabilitySet;
import org.folio.roles.domain.dto.RoleCapabilitySets;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;

@UtilityClass
public class RoleCapabilitySetUtils {

  public static RoleCapabilitySetEntity roleCapabilitySetEntity() {
    return roleCapabilitySetEntity(ROLE_ID, CAPABILITY_SET_ID);
  }

  public static RoleCapabilitySetEntity roleCapabilitySetEntity(UUID capabilitySetId) {
    return roleCapabilitySetEntity(ROLE_ID, capabilitySetId);
  }

  public static RoleCapabilitySetEntity roleCapabilitySetEntity(UUID roleId, UUID capabilitySetId) {
    var result = new RoleCapabilitySetEntity();

    result.setRoleId(roleId);
    result.setCapabilitySetId(capabilitySetId);

    return result;
  }

  public static RoleCapabilitySet roleCapabilitySet() {
    return roleCapabilitySet(ROLE_ID, CAPABILITY_SET_ID);
  }

  public static RoleCapabilitySet roleCapabilitySet(UUID capabilitySetId) {
    return roleCapabilitySet(ROLE_ID, capabilitySetId);
  }

  public static RoleCapabilitySet roleCapabilitySet(UUID roleId, UUID capabilitySetId) {
    return new RoleCapabilitySet().roleId(roleId).capabilitySetId(capabilitySetId);
  }

  public static RoleCapabilitySets roleCapabilitySets(RoleCapabilitySet... roleCapabilitySets) {
    return new RoleCapabilitySets()
      .roleCapabilitySets(Arrays.asList(roleCapabilitySets))
      .totalRecords((long) roleCapabilitySets.length);
  }

  public static RoleCapabilitySets roleCapabilitySets(long totalRecords, RoleCapabilitySet... roleCapabilitySets) {
    return new RoleCapabilitySets()
      .roleCapabilitySets(Arrays.asList(roleCapabilitySets))
      .totalRecords(totalRecords);
  }

  public static RoleCapabilitySetsRequest roleCapabilitySetsRequest(UUID roleId, UUID... capabilitySetIds) {
    return new RoleCapabilitySetsRequest().roleId(roleId).capabilitySetIds(Arrays.asList(capabilitySetIds));
  }

  public static RoleCapabilitySetsRequest roleCapabilitySetsRequest(UUID roleId, List<UUID> capabilitySetIds) {
    return new RoleCapabilitySetsRequest().roleId(roleId).capabilitySetIds(capabilitySetIds);
  }
}
