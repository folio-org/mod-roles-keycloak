package org.folio.roles.it;

import static java.time.ZoneId.systemDefault;
import static java.util.UUID.fromString;
import static org.apache.commons.collections4.IterableUtils.find;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySets;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.support.UserRoleTestUtils.userRoles;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import java.time.LocalDateTime;
import java.util.Date;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.dto.RolesRequest;
import org.folio.test.extensions.KeycloakRealms;
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
@Sql(scripts = "classpath:/sql/truncate-role-tables.sql", executionPhase = AFTER_TEST_METHOD)
class RoleKeycloakIT extends BaseIntegrationTest {

  private static final Role ROLE_1 = new Role()
    .id(fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e"))
    .name("role1")
    .description("role1_description");

  private static final Role ROLE_2 = new Role()
    .id(fromString("2e985e76-e9ca-401c-ad8e-0d121a22222e"))
    .name("role2")
    .description("role2_description");

  private static final Role ROLE_NOT_EXISTED = new Role()
    .id(fromString("3e985e76-e9ca-401c-ad8e-0d121a33333e"))
    .name("role3")
    .description("role3_description");

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
  @Sql("classpath:/sql/populate-role.sql")
  void getRole_positive() throws Exception {
    var response = mockMvc.perform(get("/roles/{id}", ROLE_1.getId()).header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(ROLE_1)))
      .andReturn();
    var role = parseResponse(response, Role.class);
    var metadata = role.getMetadata();
    assertEquals(timestampFrom("2023-01-01T12:01:01"), metadata.getCreatedDate());
    assertEquals(timestampFrom("2023-01-02T12:01:01"), metadata.getUpdatedDate());
    assertEquals("11111111-2222-1111-2222-111111111111", metadata.getCreatedByUserId().toString());
    assertEquals("11111111-1111-2222-1111-111111111111", metadata.getUpdatedByUserId().toString());
  }

  @Test
  void getRole_negative_notFound() throws Exception {
    mockMvc.perform(get("/roles/{id}", ROLE_NOT_EXISTED.getId()).header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm.json")
  void createRole_positive() throws Exception {
    var roleToCreate = new Role().name("test role").description("test description").type(RoleType.CONSORTIUM);
    var roleToCreateAsJson = asJsonString(roleToCreate);
    mockMvc.perform(post("/roles")
        .content(roleToCreateAsJson)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(roleToCreateAsJson))
      .andExpect(jsonPath("$.id").value(notNullValue()))
      .andExpect(jsonPath("$.name").value("test role"))
      .andExpect(jsonPath("$.type").value(RoleType.CONSORTIUM.toString()))
      .andExpect(jsonPath("$.metadata.createdByUserId").value(equalTo(USER_ID_HEADER)))
      .andExpect(jsonPath("$.metadata.createdDate").value(notNullValue()))
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(equalTo(USER_ID_HEADER)))
      .andExpect(jsonPath("$.metadata.updatedDate").value(notNullValue()));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm.json")
  void createRoles_positive() throws Exception {
    var response = mockMvc.perform(post("/roles/batch")
        .content(asJsonString(new RolesRequest().addRolesItem(ROLE_1).addRolesItem(ROLE_2)))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andReturn();

    var roles = parseResponse(response, Roles.class);
    assertAll(() -> {
      assertNotNull(find(roles.getRoles(),
        role -> ROLE_1.getName().equals(role.getName()) && ROLE_1.getDescription().equals(role.getDescription())));
      assertNotNull(find(roles.getRoles(),
        role -> ROLE_2.getName().equals(role.getName()) && ROLE_2.getDescription().equals(role.getDescription())));

      var role1Metadata = roles.getRoles().get(0).getMetadata();
      assertNotNull(role1Metadata.getCreatedByUserId());
      assertNotNull(role1Metadata.getCreatedDate());
      assertNotNull(role1Metadata.getUpdatedByUserId());
      assertNotNull(role1Metadata.getUpdatedDate());

      var role2Metadata = roles.getRoles().get(1).getMetadata();
      assertNotNull(role2Metadata.getCreatedByUserId());
      assertNotNull(role2Metadata.getCreatedDate());
      assertNotNull(role2Metadata.getUpdatedByUserId());
      assertNotNull(role2Metadata.getUpdatedDate());
    });
  }

  @Test
  void createRoles_negative_validationError() throws Exception {
    mockMvc.perform(post("/roles/batch").header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-roles.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role-capability-and-capability-set-for-deletion.sql",
    "classpath:/sql/populate-user-role-for-deletion.sql",
    "classpath:/sql/populate-role-policy.sql"
  })
  void deleteRole_positive() throws Exception {
    doDelete("/roles/{id}", ROLE_1.getId());

    attemptGet("/roles/{id}", ROLE_1.getId())
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));

    attemptGet("/roles/{id}/capabilities", ROLE_1.getId())
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));

    doGet("/roles/users/{userId}", fromString("8c6d12fa-33a7-48c9-8769-71168d441345"))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(userRoles())));

    doGet("/roles/capability-sets")
      .andExpect(content().json(asJsonString(roleCapabilitySets())));
  }

  @Test
  void deleteRole_negative_notFound() throws Exception {
    var notExistingRoleId = ROLE_NOT_EXISTED.getId();
    var errorMessage = "Unable to find org.folio.roles.domain.entity.RoleEntity with id " + notExistingRoleId;
    mockMvc.perform(delete("/roles/{id}", notExistingRoleId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-roles.json")
  @Sql("classpath:/sql/populate-role.sql")
  void updateRole_positive() throws Exception {
    var roleForUpdate = new Role();
    roleForUpdate.setId(ROLE_1.getId());
    var updatedDescription = "updated description";
    roleForUpdate.setDescription(updatedDescription);
    var updatedName = "updated name";
    roleForUpdate.setName(updatedName);

    mockMvc.perform(put("/roles/{id}", ROLE_1.getId())
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .content(asJsonString(roleForUpdate))
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());

    var mvcResult = doGet("/roles/{id}", ROLE_1.getId()).andReturn();
    var updatedRole = parseResponse(mvcResult, Role.class);

    var updatedRoleMetadata = updatedRole.getMetadata();
    assertEquals(updatedName, updatedRole.getName());
    assertEquals(updatedDescription, updatedRole.getDescription());
    assertEquals(USER_ID_HEADER, updatedRoleMetadata.getUpdatedByUserId().toString());
    assertEquals("11111111-2222-1111-2222-111111111111", updatedRoleMetadata.getCreatedByUserId().toString());
    assertNotNull(updatedRoleMetadata.getUpdatedDate());
    assertNotNull(updatedRoleMetadata.getCreatedDate());
  }

  @Test
  void updateRole_negative_notFoundError() throws Exception {
    var notExistingRoleId = ROLE_NOT_EXISTED.getId();
    mockMvc.perform(put("/roles/{id}", notExistingRoleId).header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(new Role().name("Updated name").description("updated description")))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm.json")
  void createRole_withNullDescription_positive() throws Exception {
    var roleToCreate = new Role().name("test role null desc");
    var roleToCreateAsJson = asJsonString(roleToCreate);
    var result = mockMvc.perform(post("/roles")
        .content(roleToCreateAsJson)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value(notNullValue()))
      .andExpect(jsonPath("$.name").value("test role null desc"))
      .andExpect(jsonPath("$.description").value(""))
      .andExpect(jsonPath("$.metadata.createdByUserId").value(equalTo(USER_ID_HEADER)))
      .andReturn();

    var createdRole = parseResponse(result, Role.class);
    assertNotNull(createdRole.getDescription());
    assertEquals("", createdRole.getDescription());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm.json")
  void createRole_withEmptyDescription_positive() throws Exception {
    var roleToCreate = new Role().name("test role empty desc").description("");
    var result = mockMvc.perform(post("/roles")
        .content(asJsonString(roleToCreate))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value(notNullValue()))
      .andExpect(jsonPath("$.name").value("test role empty desc"))
      .andExpect(jsonPath("$.description").value(""))
      .andReturn();

    var createdRole = parseResponse(result, Role.class);
    assertNotNull(createdRole.getDescription());
    assertEquals("", createdRole.getDescription());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-roles.json")
  @Sql("classpath:/sql/populate-role.sql")
  void updateRole_negative_cannotChangeTypeToDefault() throws Exception {
    var roleForUpdate = new Role();
    roleForUpdate.setId(ROLE_1.getId());
    roleForUpdate.setName(ROLE_1.getName());
    roleForUpdate.setDescription(ROLE_1.getDescription());
    roleForUpdate.setType(RoleType.DEFAULT);

    mockMvc.perform(put("/roles/{id}", ROLE_1.getId())
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(roleForUpdate))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("IllegalArgumentException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].message", is("Cannot change role type to DEFAULT.")));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-roles.json")
  @Sql("classpath:/sql/populate-default-role.sql")
  void updateRole_negative_cannotChangeTypeFromDefault() throws Exception {
    var roleForUpdate = new Role();
    roleForUpdate.setId(ROLE_NOT_EXISTED.getId());
    roleForUpdate.setName(ROLE_NOT_EXISTED.getName());
    roleForUpdate.setDescription(ROLE_NOT_EXISTED.getDescription());
    roleForUpdate.setType(RoleType.REGULAR);

    mockMvc.perform(put("/roles/{id}", ROLE_NOT_EXISTED.getId())
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(roleForUpdate))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("IllegalArgumentException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].message",
        is("Default role cannot be created, updated or deleted via roles API.")));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-roles.json")
  @Sql("classpath:/sql/populate-role.sql")
  void updateRole_positive_canChangeTypeFromRegularToConsortium() throws Exception {
    var roleForUpdate = new Role();
    roleForUpdate.setId(ROLE_1.getId());
    roleForUpdate.setName(ROLE_1.getName());
    roleForUpdate.setDescription(ROLE_1.getDescription());
    roleForUpdate.setType(RoleType.CONSORTIUM);

    mockMvc.perform(put("/roles/{id}", ROLE_1.getId())
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(roleForUpdate))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var mvcResult = doGet("/roles/{id}", ROLE_1.getId()).andReturn();
    var updatedRole = parseResponse(mvcResult, Role.class);

    assertEquals(RoleType.CONSORTIUM, updatedRole.getType());
  }

  private static Date timestampFrom(String value) {
    return Date.from(LocalDateTime.parse(value).atZone(systemDefault()).toInstant());
  }
}
