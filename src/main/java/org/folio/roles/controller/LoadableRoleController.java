package org.folio.roles.controller;

import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.rest.resource.LoadableRolesApi;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoadableRoleController implements LoadableRolesApi {

  private final LoadableRoleService service;

  @Override
  public ResponseEntity<LoadableRoles> findLoadableRoles(String query, Integer limit, Integer offset) {
    return ResponseEntity.ok(service.find(query, limit, offset));
  }

  @Override
  public ResponseEntity<LoadableRole> createLoadableRole(LoadableRole loadableRole) {
    var response = service.saveDefaultRolesIncremental(loadableRole);
    return ResponseEntity.status(201).body(response);
  }
}
