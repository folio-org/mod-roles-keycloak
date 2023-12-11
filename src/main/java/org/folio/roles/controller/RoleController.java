package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.dto.RolesRequest;
import org.folio.roles.rest.resource.RolesApi;
import org.folio.roles.service.role.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleController implements RolesApi {

  private final RoleService roleService;

  @Override
  public ResponseEntity<Role> getRole(UUID roleId) {
    var role = roleService.getById(roleId);
    return ResponseEntity.ok().body(role);
  }

  @Override
  public ResponseEntity<Roles> findRoles(String query, Integer limit, Integer offset) {
    var roles = roleService.search(query, offset, limit);
    return ResponseEntity.ok().body(roles);
  }

  @Override
  public ResponseEntity<Role> createRole(Role role) {
    return ResponseEntity.status(CREATED).body(roleService.create(role));
  }

  @Override
  public ResponseEntity<Roles> createRoles(RolesRequest roles) {
    var createdRoles = roleService.create(roles.getRoles());
    return ResponseEntity.status(CREATED).body(createdRoles);
  }

  @Override
  public ResponseEntity<Void> updateRole(UUID roleId, Role role) {
    role.setId(roleId);
    roleService.update(role);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteRole(UUID roleId) {
    roleService.deleteById(roleId);
    return ResponseEntity.noContent().build();
  }
}
