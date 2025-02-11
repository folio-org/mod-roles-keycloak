package org.folio.roles.it;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY_NAME;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY_NAME;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.INVALID_CAPABILITY_NAME;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capabilitiesUpdateRequest;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.CapabilityUtils.fooItemCapability;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilities;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilitiesRequest;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapability;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import org.folio.roles.KeycloakTestClient;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@SqlMergeMode(MERGE)
@Import(KeycloakTestClient.class)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql",
})
class RoleCapabilityIT extends BaseIntegrationTest {

  private static final UUID ROLE_ID = fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e");

  @Autowired private KeycloakTestClient kcTestClient;
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
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void findCapabilities_positive() throws Exception {
    doGet(get("/roles/capabilities")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(roleCapabilities(
        roleCapability(ROLE_ID, FOO_CREATE_CAPABILITY),
        roleCapability(ROLE_ID, FOO_VIEW_CAPABILITY)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void findCapabilities_positive_offsetAndLimit() throws Exception {
    doGet(get("/roles/capabilities")
      .param("offset", "1")
      .param("limit", "1")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        roleCapabilities(2L, roleCapability(ROLE_ID, FOO_VIEW_CAPABILITY)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void findCapabilities_positive_cqlQuery() throws Exception {
    doGet(get("/roles/capabilities")
      .param("query", "capabilityId==\"" + FOO_CREATE_CAPABILITY + "\"")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        roleCapabilities(roleCapability(ROLE_ID, FOO_CREATE_CAPABILITY)))
      ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void assignCapabilities_positive() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, FOO_DELETE_CAPABILITY, FOO_EDIT_CAPABILITY);
    var fooItemDeleteRoleCapability = roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY);
    var fooItemEditRoleCapability = roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY);

    postRoleCapabilities(request)
      .andExpect(content().json(asJsonString(roleCapabilities(fooItemDeleteRoleCapability, fooItemEditRoleCapability))))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm-scope-not-found-for-resource.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void assignCapabilities_positive_notConsistentResourcesStateInKeycloak() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, FOO_DELETE_CAPABILITY, FOO_EDIT_CAPABILITY);
    var fooItemDeleteRoleCapability = roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY);
    var fooItemEditRoleCapability = roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY);

    postRoleCapabilities(request)
      .andExpect(content().json(asJsonString(roleCapabilities(fooItemDeleteRoleCapability, fooItemEditRoleCapability))))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void assignCapabilitiesByNames_positive() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, FOO_DELETE_CAPABILITY_NAME, FOO_EDIT_CAPABILITY_NAME);
    var fooItemDeleteRoleCapability = roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY);
    var fooItemEditRoleCapability = roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY);

    postRoleCapabilities(request)
      .andExpect(content().json(asJsonString(roleCapabilities(fooItemDeleteRoleCapability, fooItemEditRoleCapability))))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql"
  })
  void assignCapabilities_positive_cleanInstallation() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, FOO_CREATE_CAPABILITY, FOO_EDIT_CAPABILITY);
    var fooItemViewRoleCapability = roleCapability(ROLE_ID, FOO_CREATE_CAPABILITY);
    var fooItemEditRoleCapability = roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY);

    postRoleCapabilities(request)
      .andExpect(content().json(asJsonString(roleCapabilities(fooItemViewRoleCapability, fooItemEditRoleCapability))))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  void assignCapabilities_negative_emptyCapabilities() throws Exception {
    attemptToPostRoleCapabilities(roleCapabilitiesRequest(ROLE_ID, emptyList()))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message")
        .value("'capabilityIds' or 'capabilityNames' must not be null"))
      .andExpect(jsonPath("$.errors[0].type").value("IllegalArgumentException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"));
  }

  @Test
  void assignCapabilitySets_negative_notFoundCapabilitySetNames() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, INVALID_CAPABILITY_NAME);
    attemptToPostRoleCapabilities(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message")
        .value("Capabilities by name are not found"))
      .andExpect(jsonPath("$.errors[0].type").value("RequestValidationException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"))
      .andExpect(jsonPath("$.errors[0].parameters[0].key").value("capabilityNames"))
      .andExpect(jsonPath("$.errors[0].parameters[0].value").value("[boo_item.create]"));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void assignCapabilities_negative_alreadyAssigned() throws Exception {
    var capabilityIds = List.of(FOO_CREATE_CAPABILITY, FOO_VIEW_CAPABILITY);
    var req = roleCapabilitiesRequest(ROLE_ID, capabilityIds);

    attemptToPostRoleCapabilities(req)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message", is(String.format(
        "Relation already exists for role='%s' and capabilities=%s", ROLE_ID, capabilityIds))))
      .andExpect(jsonPath("$.errors[0].code", is("found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityExistsException")))
      .andExpect(jsonPath("$.total_records", is(1)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void findByRoleId_positive() throws Exception {
    doGet(get("/roles/{id}/capabilities", ROLE_ID)
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilities(
        fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post", fooItemPostEndpoint()),
        fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get", fooItemGetEndpoint())
      ))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-role-capability-set-relations.sql"
  })
  void findByRoleId_positive_expandCapabilitySets() throws Exception {
    doGet(get("/roles/{id}/capabilities", ROLE_ID)
      .queryParam("expand", "true")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_CREATE_CAPABILITY, FOO_RESOURCE, CREATE, "foo.item.post", fooItemPostEndpoint()),
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint())
      ))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void update_positive() throws Exception {
    var request = capabilitiesUpdateRequest(
      FOO_VIEW_CAPABILITY,
      FOO_EDIT_CAPABILITY,
      FOO_DELETE_CAPABILITY);

    updateRoleCapabilities(request);

    doGet("/roles/capabilities")
      .andExpect(content().json(asJsonString(roleCapabilities(
        roleCapability(ROLE_ID, FOO_VIEW_CAPABILITY),
        roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY),
        roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY)))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void update_positiveByName() throws Exception {
    var request = capabilitiesUpdateRequest(FOO_EDIT_CAPABILITY_NAME, FOO_DELETE_CAPABILITY_NAME);

    updateRoleCapabilities(request);

    doGet("/roles/capabilities")
      .andExpect(content().json(asJsonString(roleCapabilities(
        roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY),
        roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY)))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void update_positive_emptyCapabilitySet() throws Exception {
    var request1 = capabilitiesUpdateRequest(FOO_EDIT_CAPABILITY_NAME, FOO_DELETE_CAPABILITY_NAME);

    updateRoleCapabilities(request1);

    doGet("/roles/capabilities")
      .andExpect(content().json(asJsonString(roleCapabilities(
        roleCapability(ROLE_ID, FOO_EDIT_CAPABILITY),
        roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY)))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));

    var request2 = new CapabilitiesUpdateRequest();

    updateRoleCapabilities(request2);

    var emptyResponse = roleCapabilities();
    doGet("/roles/capabilities")
      .andExpect(content().json(asJsonString(emptyResponse)));

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  @Test
  void update_negative_notFoundCapabilitySetNames() throws Exception {
    var request = capabilitiesUpdateRequest(INVALID_CAPABILITY_NAME);
    attemptUpdateRoleCapabilities(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message")
        .value("Capabilities by name are not found"))
      .andExpect(jsonPath("$.errors[0].type").value("RequestValidationException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"))
      .andExpect(jsonPath("$.errors[0].parameters[0].key").value("capabilityNames"))
      .andExpect(jsonPath("$.errors[0].parameters[0].value").value("[boo_item.create]"));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void deleteCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/roles/{id}/capabilities", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/roles/capabilities").andExpect(content().json(asJsonString(roleCapabilities())));

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql"
  })
  void deleteCapabilities_negative_nothingToDelete() throws Exception {
    var expectedErrorMessage = "Relations between role and capabilities are not found for role: " + ROLE_ID;
    mockMvc.perform(delete("/roles/{id}/capabilities", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(expectedErrorMessage)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = "classpath:/sql/capabilities/populate-capabilities.sql")
  void assignCapabilities_positive_roleNameIsChanged() throws Exception {
    var testRole = new Role().name("Test User Role").description("Test user role description");
    var testRoleJson = asJsonString(testRole);
    var roleResponse = mockMvc.perform(post("/roles")
        .content(testRoleJson)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(testRoleJson))
      .andReturn();

    var roleId = parseResponse(roleResponse, Role.class).getId();
    postRoleCapabilities(roleCapabilitiesRequest(roleId, List.of(FOO_CREATE_CAPABILITY)));

    mockMvc.perform(put("/roles/{id}", roleId)
        .content(asJsonString(testRole.name("Test User Role Updated")))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    postRoleCapabilities(roleCapabilitiesRequest(roleId, List.of(FOO_VIEW_CAPABILITY)));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(roleId, fooItemPostEndpoint()),
      kcPermissionName(roleId, fooItemGetEndpoint())
    ));
  }

  static void updateRoleCapabilities(CapabilitiesUpdateRequest request) throws Exception {
    attemptUpdateRoleCapabilities(request).andExpect(status().isNoContent());
  }

  static ResultActions attemptUpdateRoleCapabilities(CapabilitiesUpdateRequest request) throws Exception {
    return mockMvc.perform(put("/roles/{id}/capabilities", ROLE_ID)
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .contentType(APPLICATION_JSON)
      .content(asJsonString(request)));
  }

  static ResultActions postRoleCapabilities(RoleCapabilitiesRequest request) throws Exception {
    return attemptToPostRoleCapabilities(request).andExpect(status().isCreated());
  }

  private static ResultActions attemptToPostRoleCapabilities(RoleCapabilitiesRequest request) throws Exception {
    return mockMvc.perform(post("/roles/capabilities")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON));
  }

  protected static String kcPermissionName(Endpoint endpoint) {
    return kcPermissionName(ROLE_ID, endpoint);
  }

  protected static String kcPermissionName(UUID roleId, Endpoint endpoint) {
    return String.format("%s access for role '%s' to '%s'", endpoint.getMethod(), roleId, endpoint.getPath());
  }
}
