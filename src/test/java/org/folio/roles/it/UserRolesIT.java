package org.folio.roles.it;

import static java.util.UUID.fromString;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.support.UserRoleTestUtils.userRole;
import static org.folio.roles.support.UserRoleTestUtils.userRoles;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@KeycloakRealms("classpath:json/keycloak/test-realm-roles-users.json")
@Sql(scripts = "classpath:/sql/truncate-roles-user-related-tables.sql", executionPhase = AFTER_TEST_METHOD)
class UserRolesIT extends BaseIntegrationTest {

  public static final UUID USER_UUID = fromString("61893f40-4739-49fc-bf07-daeff3021f90");
  public static final UUID ROLE_UUID_1 = fromString("5f2492dd-adcd-445b-9118-bcfa9b406c95");
  public static final UUID ROLE_UUID_2 = fromString("5f2492dd-adcd-445b-9118-bcfa9b406c22");

  @Autowired private Keycloak keycloak;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @BeforeEach
  void setUp() {
    keycloak.tokenManager().grantToken();
  }

  @Test
  @Sql("classpath:/sql/populate-roles-user-tables.sql")
  void getRolesUser_positive() throws Exception {
    mockMvc.perform(get("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(readTemplate("[userRole] findByUserId-response.json")));
  }

  @Test
  void getRolesUser_negative_notFound() throws Exception {
    mockMvc.perform(get("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.userRoles", emptyIterable()))
      .andExpect(jsonPath("$.totalRecords", is(0)));
  }

  @Test
  @Sql("classpath:/sql/populate-roles-for-roles-user-tables.sql")
  @WireMockStub(scripts = {"/wiremock/stubs/moduserskc/ensure-kc-user.json"})
  void assignRolesToUser_positive() throws Exception {
    var expectedUserRolesJson = asJsonString(userRoles(userRole(USER_UUID, ROLE_UUID_1)));
    mockMvc.perform(post("/roles/users")
        .content(asJsonString(new UserRolesRequest().userId(USER_UUID).addRoleIdsItem(ROLE_UUID_1)))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(expectedUserRolesJson))
      .andExpect(jsonPath("$.userRoles[0].metadata.createdByUserId").value(USER_ID_HEADER))
      .andExpect(jsonPath("$.userRoles[0].metadata.createdDate").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedByUserId").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedDate").exists());

    doGet("/roles/users/{id}", USER_UUID)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(expectedUserRolesJson))
      .andExpect(jsonPath("$.userRoles[0].metadata.createdByUserId").value(USER_ID_HEADER))
      .andExpect(jsonPath("$.userRoles[0].metadata.createdDate").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedByUserId").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedDate").exists());
  }

  @Test
  void create_roles_negative_validation_error() throws Exception {
    mockMvc.perform(post("/roles/users")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @Sql("classpath:/sql/populate-roles-user-tables.sql")
  void deleteRolesUser_positive() throws Exception {
    mockMvc.perform(delete("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/roles/users/{id}", USER_UUID)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.userRoles", emptyIterable()))
      .andExpect(jsonPath("$.totalRecords", is(0)));
  }

  @Test
  void deleteRolesUser_negative_notFoundError() throws Exception {
    mockMvc.perform(delete("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].message", is("There are no assigned roles for userId: " + USER_UUID)));
  }

  @Test
  @Sql("classpath:/sql/populate-roles-user-tables.sql")
  void updateRolesUser_positive() throws Exception {
    mockMvc.perform(put("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(new UserRolesRequest().userId(USER_UUID).roleIds(List.of(ROLE_UUID_2))))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    doGet("/roles/users/{userId}", USER_UUID)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(readTemplate("[userRole] update-response.json")))
      .andExpect(jsonPath("$.userRoles[0].metadata.createdDate").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.createdByUserId").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedDate").exists())
      .andExpect(jsonPath("$.userRoles[0].metadata.updatedByUserId").exists());
  }

  @Test
  void updateRolesUser_negative_notFound() throws Exception {
    var unknownUuid = UUID.randomUUID();
    mockMvc.perform(put("/roles/users/{userId}", USER_UUID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(new UserRolesRequest().userId(USER_UUID).addRoleIdsItem(unknownUuid)))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].message", matchesPattern("Roles are not found for ids: \\[.*]")));
  }
}
