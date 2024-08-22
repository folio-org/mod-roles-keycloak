package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitySets;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.dto.RolePermissionNamesRequest;
import org.folio.roles.rest.resource.RoleCapabilitySetApi;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.role.RoleService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleCapabilitySetController implements RoleCapabilitySetApi {

  private final RoleService roleService;
  private final CapabilitySetService capabilitySetService;
  @Qualifier("apiRoleCapabilitySetService")
  private final RoleCapabilitySetService roleCapabilitySetService;

  @Override
  public ResponseEntity<RoleCapabilitySets> createRoleCapabilitySets(RoleCapabilitySetsRequest request) {
    var pageResult = roleCapabilitySetService.create(request.getRoleId(), request.getCapabilitySetIds());
    return ResponseEntity.status(CREATED).body(new RoleCapabilitySets()
      .roleCapabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<RoleCapabilitySets> createRoleCapabilitySetsByPermissionNames(
    RolePermissionNamesRequest rolePermissionNamesRequest) {
    var capabilitySetIds = capabilitySetService.findByPermissionNames(rolePermissionNamesRequest.getPermissionNames())
      .stream()
      .map(CapabilitySet::getId)
      .toList();
    var roleCapabilitySetsRequest = new RoleCapabilitySetsRequest()
      .roleId(rolePermissionNamesRequest.getRoleId())
      .capabilitySetIds(capabilitySetIds);

    return createRoleCapabilitySets(roleCapabilitySetsRequest);
  }

  @Override
  public ResponseEntity<CapabilitySets> getCapabilitySetsByRoleId(UUID roleId, Integer limit, Integer offset) {
    roleService.getById(roleId);
    var pageResult = capabilitySetService.findByRoleId(roleId, limit, offset);
    return ResponseEntity.ok(new CapabilitySets()
      .capabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<RoleCapabilitySets> getRoleCapabilitySets(String query, Integer limit, Integer offset) {
    var pageResult = roleCapabilitySetService.find(query, limit, offset);
    return ResponseEntity.ok(new RoleCapabilitySets()
      .roleCapabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Void> updateRoleCapabilitySets(UUID roleId, CapabilitySetsUpdateRequest request) {
    roleCapabilitySetService.update(roleId, request.getCapabilitySetIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteRoleCapabilitySets(UUID roleId) {
    roleCapabilitySetService.deleteAll(roleId);
    return ResponseEntity.noContent().build();
  }
}
