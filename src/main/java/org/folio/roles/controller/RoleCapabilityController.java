package org.folio.roles.controller;

import static java.lang.Boolean.TRUE;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilities;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.rest.resource.RoleCapabilityApi;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.role.RoleService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleCapabilityController implements RoleCapabilityApi {

  private final RoleService roleEntityService;
  private final CapabilityService capabilityService;
  @Qualifier("apiRoleCapabilityService")
  private final RoleCapabilityService roleCapabilityService;

  @Override
  public ResponseEntity<RoleCapabilities> createRoleCapabilities(RoleCapabilitiesRequest request) {
    var pageResult = roleCapabilityService.create(request, false);
    return ResponseEntity.status(CREATED).body(new RoleCapabilities()
      .roleCapabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Capabilities> findCapabilitiesByRoleId(UUID id, Boolean expand, Integer limit, Integer offset) {
    roleEntityService.getById(id);
    var pageResult = capabilityService.findByRoleId(id, TRUE.equals(expand), limit, offset);
    return ResponseEntity.ok(new Capabilities()
      .capabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<RoleCapabilities> getRoleCapabilities(String query, Integer limit, Integer offset) {
    var pageResult = roleCapabilityService.find(query, limit, offset);
    return ResponseEntity.ok(new RoleCapabilities()
      .roleCapabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Void> updateRoleCapabilities(UUID roleId, CapabilitiesUpdateRequest request) {
    roleCapabilityService.update(roleId, request);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteRoleCapabilities(UUID roleId) {
    roleCapabilityService.deleteAll(roleId);
    return ResponseEntity.noContent().build();
  }
}
