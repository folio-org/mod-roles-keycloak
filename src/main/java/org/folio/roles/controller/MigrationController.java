package org.folio.roles.controller;

import lombok.RequiredArgsConstructor;
import org.folio.roles.rest.resource.MigrateApi;
import org.folio.roles.service.permission.PermissionMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MigrationController implements MigrateApi {

  private final PermissionMigrationService permissionMigrationService;

  @Override
  public ResponseEntity<Void> migratePolicies() {
    permissionMigrationService.migratePermissions();
    return ResponseEntity.noContent().build();
  }
}
