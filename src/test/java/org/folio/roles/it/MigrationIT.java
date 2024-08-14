package org.folio.roles.it;

import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.roles.base.BaseIntegrationTest;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(scripts = "classpath:/sql/truncate-tables-after-migration.sql", executionPhase = AFTER_TEST_METHOD)
class MigrationIT extends BaseIntegrationTest {

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/user-permissions-for-migrations-1.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-2.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-3.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-4.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-5.json",
    "/wiremock/stubs/moduserskc/ensure-kc-user.json"
  })
  @KeycloakRealms("/json/keycloak/test-realm-for-migration.json")
  void getPolicy_positive() throws Exception {
    attemptPost("/roles-keycloak/migrate", null).andExpect(status().isNoContent());
  }
}
