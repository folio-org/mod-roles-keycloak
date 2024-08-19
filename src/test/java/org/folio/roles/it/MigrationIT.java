package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestUtils.await;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql",
})
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
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/user-permissions-for-migrations-1.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-2.json",
    "/wiremock/stubs/perms/user-permissions-for-migrations-3.json",
    "/wiremock/stubs/moduserskc/ensure-kc-user.json"
  })
  @KeycloakRealms("/json/keycloak/test-realm-for-migration.json")
  void migratePolicies_positive() throws Exception {
    var mvcResult = attemptPost("/roles-keycloak/migrations", null).andExpect(status().isCreated()).andReturn();
    var permissionMigrationJob = parseResponse(mvcResult, PermissionMigrationJob.class);

    var migrationId = permissionMigrationJob.getId();

    await()
      .pollInterval(FIVE_HUNDRED_MILLISECONDS)
      .atMost(TEN_SECONDS)
      .untilAsserted(() -> doGet("/roles-keycloak/migrations/" + migrationId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("finished"))));

    var rolesMvcResult = doGet("/roles")
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andReturn();

    var roles = parseResponse(rolesMvcResult, Roles.class).getRoles();

    var role1Name = "cea4e0d50e51ef2dfce2b64c11ebf4e086edbf3a";
    var role2Name = "38e3e6e15ef923be5dbccfca79579807ac09e126";

    assertThat(mapItems(roles, Role::getName)).containsExactlyInAnyOrder(role2Name, role1Name);

    var role1Id = getRoleId(role1Name, roles);
    assertThat(role1Id).isNotNull();
    doGet("/roles/{id}/capabilities", role1Id)
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.capabilities[*].permission",
        containsInAnyOrder("foo.item.get", "foo.item.post", "foo.item.all")));

    doGet("/roles/{id}/capability-sets", role1Id)
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.capabilitySets[*].permission", containsInAnyOrder("foo.item.all", "ui-foo.item.edit")));

    var role2Id = getRoleId(role2Name, roles);
    assertThat(role1Id).isNotNull();
    doGet("/roles/{id}/capabilities", role2Id)
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.capabilities[*].permission", containsInAnyOrder("foo.item.get")));

    doGet("/roles/{id}/capability-sets", role2Id)
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.totalRecords", is(0)));
  }

  private static UUID getRoleId(String name, List<Role> roles) {
    return toStream(roles)
      .filter(role -> Objects.equals(role.getName(), name))
      .map(Role::getId)
      .findFirst()
      .orElse(null);
  }
}
