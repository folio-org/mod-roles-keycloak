package org.folio.roles.support;

import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.dto.UserRoles;
import org.folio.roles.domain.dto.UserRolesRequest;

@UtilityClass
public class UserRoleTestUtils {

  public static UserRolesRequest userRolesRequest() {
    return userRolesRequest(USER_ID, ROLE_ID);
  }

  public static UserRolesRequest userRolesRequest(List<UUID> roleIds) {
    var request = new UserRolesRequest();
    request.setUserId(USER_ID);
    request.setRoleIds(roleIds);
    return request;
  }

  public static UserRolesRequest userRolesRequest(UUID userId, UUID... roleIds) {
    var request = new UserRolesRequest();
    request.setUserId(userId);
    request.setRoleIds(Arrays.asList(roleIds));
    return request;
  }

  public static UserRoles userRoles(UserRole... userRoles) {
    var userRolesList = Arrays.asList(userRoles);
    var request = new UserRoles();
    request.setUserRoles(userRolesList);
    request.setTotalRecords(userRolesList.size());
    return request;
  }

  public static UserRoles userRoles(List<UserRole> roleUsers) {
    var request = new UserRoles();
    request.setUserRoles(roleUsers);
    request.setTotalRecords(CollectionUtils.isEmpty(roleUsers) ? 0 : roleUsers.size());
    return request;
  }

  public static UserRole userRole() {
    return userRole(USER_ID, ROLE_ID);
  }

  public static UserRole userRole(UUID roleId) {
    return userRole(USER_ID, roleId);
  }

  public static UserRole userRole(UUID userId, UUID roleId) {
    var userRole = new UserRole();
    userRole.setUserId(userId);
    userRole.setRoleId(roleId);
    return userRole;
  }
}
