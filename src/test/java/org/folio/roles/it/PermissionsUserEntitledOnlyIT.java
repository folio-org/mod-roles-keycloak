package org.folio.roles.it;

import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = {
  "/sql/capabilities/populate-capabilities.sql",
  "/sql/capability-sets/populate-capability-sets.sql",
  "/sql/populate-user-capability-relations.sql",
  "/sql/capabilities/populate-capability-permissions.sql",
  "/sql/capabilities/update-capabilities-for-entitlement-tests.sql"
})
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "/sql/truncate-capability-tables.sql",
  "/sql/truncate-role-tables.sql",
  "/sql/truncate-role-capability-tables.sql",
  "/sql/truncate-user-capability-tables.sql",
  "/sql/truncate-roles-user-related-tables.sql",
  "/sql/truncate-permission-table.sql"
})
class PermissionsUserEntitledOnlyIT extends BaseIntegrationTest {

  // user1: has capabilities from both test-application-0.0.1 and test-application-2.0.0 (after SQL update)
  private static final UUID USER_ID_1 = UUID.fromString("cf078e4a-5d9c-45f1-9c1d-f87003790d9f");
  // user4: has only role1 capabilities (foo_item.delete + ui_foo_item.delete) - mixed applications
  private static final UUID USER_ID_4 = UUID.fromString("c2bdde31-e216-43f7-abe3-54b6415d7472");

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/mte/entitled-applications-default.json"})
  void getPermissionsUser_positive_entitledOnly_filtersOutNotEntitledAppPermissions() throws Exception {
    // test-application-2.0.0 contains ui_foo_item.delete and ui_foo_item.create after SQL update
    // MTE stub returns only test-application-0.0.1 as entitled
    // So ui-foo.item.delete (from ui_foo_item.delete) and module.foo.item.post (from ui_foo_item.create)
    // should be excluded from results
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_1)
        .queryParam("entitledOnly", "true")
        .headers(okapiHeaders())
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.permissions", not(hasItem("module.foo.item.post"))))
      .andExpect(jsonPath("$.permissions", not(hasItem("ui-foo.item.delete"))));
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/mte/entitled-applications-default.json"})
  void getPermissionsUser_positive_entitledOnly_retainsEntitledAppPermissions() throws Exception {
    // Permissions from test-application-0.0.1 (entitled) should still be returned
    // user4 has role1: foo_item.delete (from test-application-0.0.1) + ui_foo_item.delete (from test-application-2.0.0)
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_4)
        .queryParam("entitledOnly", "true")
        .headers(okapiHeaders())
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.permissions", hasItem("foo.item.delete")))
      .andExpect(jsonPath("$.permissions", not(hasItem("ui-foo.item.delete"))));
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/mte/entitled-applications-empty.json"})
  void getPermissionsUser_positive_entitledOnly_emptyEntitlements_returnsNoPermissions() throws Exception {
    // MTE returns empty entitlements — all permissions should be filtered out
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_4)
        .queryParam("entitledOnly", "true")
        .headers(okapiHeaders())
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.permissions").isEmpty());
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/mte/entitled-applications-default.json"})
  void getPermissionsUser_positive_entitledOnlyFalse_returnsAllPermissions() throws Exception {
    // entitledOnly=false (default) — filtering should not be applied
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_4)
        .queryParam("entitledOnly", "false")
        .headers(okapiHeaders())
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.permissions", hasItem("ui-foo.item.delete")))
      .andExpect(jsonPath("$.permissions", hasItem("foo.item.delete")));
  }
}
