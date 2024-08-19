package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.PermissionMigrationJobs;
import org.folio.roles.rest.resource.MigrationApi;
import org.folio.roles.service.MigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MigrationController implements MigrationApi {

  private final MigrationService migrationService;

  @Override
  public ResponseEntity<String> deleteMigration(UUID id) {
    migrationService.deleteMigrationById(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<PermissionMigrationJobs> findMigrations(String query, Integer offset, Integer limit) {
    var migrations = migrationService.findMigrations(query, offset, limit);
    return ResponseEntity.ok(migrations);
  }

  @Override
  public ResponseEntity<PermissionMigrationJob> getMigration(UUID id) {
    var migrationById = migrationService.getMigrationById(id);
    return ResponseEntity.ok(migrationById);
  }

  @Override
  public ResponseEntity<PermissionMigrationJob> migratePermissions() {
    var migration = migrationService.createMigration();
    return ResponseEntity.status(CREATED).body(migration);
  }
}
