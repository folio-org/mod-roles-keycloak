package org.folio.roles.controller;

import static org.folio.roles.domain.dto.PermissionMigrationJobStatus.IN_PROGRESS;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.PermissionMigrationJobs;
import org.folio.roles.service.migration.MigrationErrorService;
import org.folio.roles.service.migration.MigrationService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(MigrationController.class)
@Import({ControllerTestConfiguration.class, MigrationController.class})
class MigrationControllerTest {

  private static final UUID MIGRATION_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @MockBean private MigrationService migrationService;
  @MockBean private MigrationErrorService migrationErrorService;

  @Test
  void getMigration_positive() throws Exception {
    var permissionMigrationJob = permissionMigrationJob();
    when(migrationService.getMigrationById(MIGRATION_ID)).thenReturn(permissionMigrationJob);

    mockMvc.perform(get("/roles-keycloak/migrations/{id}", MIGRATION_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(permissionMigrationJob), true));
  }

  @Test
  void searchMigrations_positive() throws Exception {
    var query = "cql.allRecords = 1";
    var permissionMigrationJobs = new PermissionMigrationJobs()
      .migrations(List.of(permissionMigrationJob()))
      .totalRecords(3);
    when(migrationService.findMigrations(query, 2, 55)).thenReturn(permissionMigrationJobs);

    mockMvc.perform(get("/roles-keycloak/migrations")
        .queryParam("query", query)
        .queryParam("offset", "2")
        .queryParam("limit", "55")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(permissionMigrationJobs), true));
  }

  @Test
  void deleteMigration_positive() throws Exception {
    mockMvc.perform(delete("/roles-keycloak/migrations/{id}", MIGRATION_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(migrationService).deleteMigrationById(MIGRATION_ID);
  }

  @Test
  void createMigration_positive() throws Exception {
    var permissionMigrationJob = permissionMigrationJob();

    when(migrationService.createMigration()).thenReturn(permissionMigrationJob);

    mockMvc.perform(post("/roles-keycloak/migrations")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(permissionMigrationJob), true));
  }

  private static PermissionMigrationJob permissionMigrationJob() {
    var permissionMigrationJob = new PermissionMigrationJob();
    permissionMigrationJob.setId(MIGRATION_ID);
    permissionMigrationJob.setStatus(IN_PROGRESS);
    return permissionMigrationJob;
  }
}
