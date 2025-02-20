package org.folio.roles.controller;

import static java.lang.Boolean.TRUE;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.UserCapabilities;
import org.folio.roles.domain.dto.UserCapabilitiesRequest;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.rest.resource.UserCapabilityApi;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.UserCapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserCapabilityController implements UserCapabilityApi {

  private final CapabilityService capabilityService;
  private final KeycloakUserService keycloakUserService;
  private final UserCapabilityService userCapabilityService;

  @Override
  public ResponseEntity<UserCapabilities> createUserCapabilities(UserCapabilitiesRequest request) {
    var pageResult = userCapabilityService.create(request.getUserId(), request.getCapabilityIds());
    return ResponseEntity.status(CREATED).body(new UserCapabilities()
      .userCapabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Capabilities> findCapabilitiesByUserId(UUID id, Boolean expand,
    Boolean includeDummy, Integer limit, Integer offset) {
    keycloakUserService.getKeycloakUserByUserId(id);
    var capabilities = capabilityService.findByUserId(id, TRUE.equals(expand),
      TRUE.equals(includeDummy), limit, offset);
    return ResponseEntity.ok(new Capabilities()
      .capabilities(capabilities.getRecords())
      .totalRecords(capabilities.getTotalRecords()));
  }

  @Override
  public ResponseEntity<UserCapabilities> getUserCapabilities(String query, Integer limit, Integer offset) {
    var pageResult = userCapabilityService.find(query, limit, offset);
    return ResponseEntity.ok(new UserCapabilities()
      .userCapabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Void> updateUserCapabilities(UUID userId, CapabilitiesUpdateRequest request) {
    userCapabilityService.update(userId, request.getCapabilityIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteUserCapabilities(UUID userId) {
    userCapabilityService.deleteAll(userId);
    return ResponseEntity.noContent().build();
  }
}
