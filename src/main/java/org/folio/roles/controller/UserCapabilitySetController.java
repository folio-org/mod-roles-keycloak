package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.UserCapabilitySets;
import org.folio.roles.domain.dto.UserCapabilitySetsRequest;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.rest.resource.UserCapabilitySetApi;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.UserCapabilitySetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserCapabilitySetController implements UserCapabilitySetApi {

  private final KeycloakUserService keycloakUserService;
  private final CapabilitySetService capabilitySetService;
  private final UserCapabilitySetService userCapabilitySetService;

  @Override
  public ResponseEntity<UserCapabilitySets> createUserCapabilitySets(UserCapabilitySetsRequest request) {
    var pageResult = userCapabilitySetService.create(request.getUserId(), request.getCapabilitySetIds());
    return ResponseEntity.status(CREATED).body(new UserCapabilitySets()
      .userCapabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<UserCapabilitySets> getUserCapabilitySets(String query, Integer limit, Integer offset) {
    var pageResult = userCapabilitySetService.find(query, limit, offset);
    return ResponseEntity.ok(new UserCapabilitySets()
      .userCapabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<CapabilitySets> getCapabilitySetsByUserId(UUID userId, Integer limit, Integer offset) {
    keycloakUserService.getKeycloakUserByUserId(userId);
    var capabilities = capabilitySetService.findByUserId(userId, limit, offset);
    return ResponseEntity.ok(new CapabilitySets()
      .capabilitySets(capabilities.getRecords())
      .totalRecords(capabilities.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Void> updateUserCapabilitySets(UUID userId, CapabilitySetsUpdateRequest request) {
    userCapabilitySetService.update(userId, request.getCapabilitySetIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteUserCapabilitySets(UUID userId) {
    userCapabilitySetService.deleteAll(userId);
    return ResponseEntity.noContent().build();
  }
}
