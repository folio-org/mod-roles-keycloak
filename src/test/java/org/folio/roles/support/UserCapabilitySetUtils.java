package org.folio.roles.support;

import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.UserCapabilitySet;
import org.folio.roles.domain.dto.UserCapabilitySets;
import org.folio.roles.domain.dto.UserCapabilitySetsRequest;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;

@UtilityClass
public class UserCapabilitySetUtils {

  public static UserCapabilitySetEntity userCapabilitySetEntity() {
    return userCapabilitySetEntity(USER_ID, CAPABILITY_SET_ID);
  }

  public static UserCapabilitySetEntity userCapabilitySetEntity(UUID capabilityId) {
    return userCapabilitySetEntity(USER_ID, capabilityId);
  }

  public static UserCapabilitySetEntity userCapabilitySetEntity(UUID roleId, UUID capabilityId) {
    var result = new UserCapabilitySetEntity();

    result.setUserId(roleId);
    result.setCapabilitySetId(capabilityId);

    return result;
  }

  public static UserCapabilitySet userCapabilitySet() {
    return userCapabilitySet(USER_ID, CAPABILITY_SET_ID);
  }

  public static UserCapabilitySet userCapabilitySet(UUID capabilitySetId) {
    return userCapabilitySet(USER_ID, capabilitySetId);
  }

  public static UserCapabilitySet userCapabilitySet(UUID userId, UUID capabilitySetId) {
    return new UserCapabilitySet().userId(userId).capabilitySetId(capabilitySetId);
  }

  public static UserCapabilitySets userCapabilitySets(UserCapabilitySet... values) {
    return userCapabilitySets(values.length, values);
  }

  public static UserCapabilitySets userCapabilitySets(long totalRecords, UserCapabilitySet... values) {
    return new UserCapabilitySets().userCapabilitySets(List.of(values)).totalRecords(totalRecords);
  }

  public static UserCapabilitySetsRequest userCapabilitySetsRequest(UUID userId, UUID... capabilitySetIds) {
    return new UserCapabilitySetsRequest().userId(userId).capabilitySetIds(Arrays.asList(capabilitySetIds));
  }

  public static UserCapabilitySetsRequest userCapabilitySetsRequest(UUID userId, List<UUID> capabilitySetIds) {
    return new UserCapabilitySetsRequest().userId(userId).capabilitySetIds(capabilitySetIds);
  }
}
