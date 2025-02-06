package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.UserRoles;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.roles.rest.resource.RolesUsersApi;
import org.folio.roles.service.role.UserRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserRoleController implements RolesUsersApi {

  private final UserRoleService userRoleService;

  @Override
  public ResponseEntity<UserRoles> assignRolesToUser(UserRolesRequest userRolesRequest) {
    var createdUserRole = userRoleService.create(userRolesRequest);
    return ResponseEntity.status(CREATED).body(createdUserRole);
  }

  @Override
  public ResponseEntity<Void> updateUserRoles(UUID userId, UserRolesRequest request) {
    Assert.isTrue(Objects.equals(userId, request.getUserId()), "User id in request and in the path must be equal");
    userRoleService.update(request);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<UserRoles> getUserRoles(UUID userId) {
    var rolesUser = userRoleService.findById(userId);
    return ResponseEntity.ok(rolesUser);
  }

  @Override
  public ResponseEntity<Void> deleteUserRoles(UUID userId) {
    userRoleService.deleteById(userId);
    return ResponseEntity.noContent().build();
  }
}
