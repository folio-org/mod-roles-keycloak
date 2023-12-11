package org.folio.roles.controller;

import static java.lang.Boolean.TRUE;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.PermissionsUser;
import org.folio.roles.rest.resource.PermissionsUsersApi;
import org.folio.roles.service.capability.CapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PermissionsUsersController implements PermissionsUsersApi {

  private final CapabilityService capabilityService;

  @Override
  public ResponseEntity<PermissionsUser> getPermissionsUser(UUID userId, Boolean onlyVisible) {
    var userPermissions = capabilityService.getUserPermissions(userId, TRUE.equals(onlyVisible));
    return ResponseEntity.ok(new PermissionsUser().userId(userId).permissions(userPermissions));
  }
}
