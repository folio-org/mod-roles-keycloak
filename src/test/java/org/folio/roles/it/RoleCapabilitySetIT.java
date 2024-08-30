package org.folio.roles.it;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.it.RoleCapabilityIT.postRoleCapabilities;
import static org.folio.roles.it.RoleCapabilityIT.updateRoleCapabilities;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.INVALID_CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetsUpdateRequest;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.capabilitiesUpdateRequest;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySet;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySets;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetsRequest;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilities;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilitiesRequest;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapability;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
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
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
  "classpath:/sql/truncate-role-capability-tables.sql"
})
class RoleCapabilitySetIT extends BaseIntegrationTest {

  private static final UUID ROLE_ID = fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e");

  @Autowired private KeycloakTestClient kcTestClient;

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
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void findCapabilitySets_positive() throws Exception {
    doGet(get("/roles/capability-sets")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(roleCapabilitySets(
        roleCapabilitySet(ROLE_ID, FOO_CREATE_CAPABILITY_SET)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-role-capability-set-relations.sql"
  })
  void findCapabilitySets_positive_offsetAndLimit() throws Exception {
    doGet(get("/roles/capability-sets")
      .param("offset", "1")
      .param("limit", "1")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        roleCapabilitySets(2L, roleCapabilitySet(ROLE_ID, FOO_EDIT_CAPABILITY_SET)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-role-capability-set-relations.sql"
  })
  void findCapabilitySets_positive_cqlQuery() throws Exception {
    doGet(get("/roles/capability-sets")
      .param("query", "capabilitySetId==\"" + FOO_CREATE_CAPABILITY_SET + "\"")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(roleCapabilitySets(
        roleCapabilitySet(ROLE_ID, FOO_CREATE_CAPABILITY_SET)))
      ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void assignCapabilitySets_positive() throws Exception {
    var request = roleCapabilitySetsRequest(ROLE_ID, FOO_EDIT_CAPABILITY_SET, FOO_CREATE_CAPABILITY_SET);
    var fooItemEditCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_EDIT_CAPABILITY_SET);
    var fooItemCreateCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_CREATE_CAPABILITY_SET);
    var expectedRoleCapabilitySets = roleCapabilitySets(fooItemEditCapabilitySet, fooItemCreateCapabilitySet);

    postRoleCapabilitySets(request)
      .andExpect(content().json(asJsonString(expectedRoleCapabilitySets)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemGetEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void assignCapabilitySetsByNames_positive() throws Exception {
    var request = roleCapabilitySetsRequest(ROLE_ID, FOO_CREATE_CAPABILITY_SET_NAME, FOO_EDIT_CAPABILITY_SET_NAME);
    var fooItemEditCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_EDIT_CAPABILITY_SET);
    var fooItemCreateCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_CREATE_CAPABILITY_SET);
    var expectedRoleCapabilitySets = roleCapabilitySets(fooItemEditCapabilitySet, fooItemCreateCapabilitySet);

    postRoleCapabilitySets(request)
      .andExpect(content().json(asJsonString(expectedRoleCapabilitySets)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemGetEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void assignCapabilities_positive_cleanInstallation() throws Exception {
    var request = roleCapabilitySetsRequest(ROLE_ID, FOO_EDIT_CAPABILITY_SET, FOO_CREATE_CAPABILITY_SET);
    var fooItemEditCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_EDIT_CAPABILITY_SET);
    var fooItemCreateCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_CREATE_CAPABILITY_SET);
    var expectedRoleCapabilitySets = roleCapabilitySets(fooItemEditCapabilitySet, fooItemCreateCapabilitySet);

    postRoleCapabilitySets(request)
      .andExpect(content().json(asJsonString(expectedRoleCapabilitySets)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdByUserId", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.roleCapabilitySets[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint())
    ));
  }

  @Test
  void assignCapabilitySets_negative_notFoundCapabilitySetNames() throws Exception {
    var request = roleCapabilitySetsRequest(ROLE_ID, INVALID_CAPABILITY_SET_NAME);
    attemptToPostRoleCapabilitySets(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message")
        .value("Capability sets by name are not found"))
      .andExpect(jsonPath("$.errors[0].type").value("RequestValidationException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"))
      .andExpect(jsonPath("$.errors[0].parameters[0].key").value("capabilitySetNames"))
      .andExpect(jsonPath("$.errors[0].parameters[0].value").value("[boo_item.create]"));
  }

  @Test
  void assignCapabilitySets_negative_emptyCapabilitySet() throws Exception {
    var request = roleCapabilitySetsRequest(ROLE_ID, emptyList());
    attemptToPostRoleCapabilitySets(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message")
        .value("'capabilitySetIds' or 'capabilitySetNames' must not be null"))
      .andExpect(jsonPath("$.errors[0].type").value("IllegalArgumentException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  void assignCapabilities_negative_alreadyAssigned() throws Exception {
    var capabilitySetIds = List.of(FOO_CREATE_CAPABILITY_SET);
    var request = roleCapabilitySetsRequest(ROLE_ID, capabilitySetIds);

    attemptToPostRoleCapabilitySets(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityExistsException")))
      .andExpect(jsonPath("$.errors[0].message", is(String.format(
        "Relation already exists for role='%s' and capabilitySets=%s", ROLE_ID, capabilitySetIds))
      ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-role-capability-set-relations.sql"
  })
  void findByRoleId_positive() throws Exception {
    var expectedEditCapabilityIds = List.of(FOO_VIEW_CAPABILITY, FOO_EDIT_CAPABILITY);
    var expectedCreateCapabilityIds = List.of(FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY);

    doGet(get("/roles/{id}/capability-sets", ROLE_ID)
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilitySets(
        capabilitySet(FOO_EDIT_CAPABILITY_SET, FOO_RESOURCE, EDIT, expectedEditCapabilityIds),
        capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, expectedCreateCapabilityIds)
      ))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void update_positive() throws Exception {
    var request = capabilitySetsUpdateRequest(FOO_EDIT_CAPABILITY_SET);
    updateRoleCapabilitySets(request);

    var fooItemEditCapabilitySet = roleCapabilitySet(ROLE_ID, FOO_EDIT_CAPABILITY_SET);
    doGet("/roles/capability-sets")
      .andExpect(content().json(asJsonString(roleCapabilitySets(fooItemEditCapabilitySet))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void deleteCapabilitySets_positive() throws Exception {
    mockMvc.perform(delete("/roles/{id}/capability-sets", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/roles/capability-sets")
      .andExpect(content().json(asJsonString(roleCapabilitySets())));

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void deleteCapabilitySets_negative_nothingToDelete() throws Exception {
    var expectedErrorMessage = "Relations between role and capability sets are not found for role: " + ROLE_ID;
    mockMvc.perform(delete("/roles/{id}/capability-sets", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(expectedErrorMessage)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void deleteCapabilitySets_positive_shouldKeepDirectlyAssignedCapabilities() throws Exception {
    mockMvc.perform(delete("/roles/{id}/capability-sets", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/roles/capability-sets")
      .andExpect(content().json(asJsonString(roleCapabilitySets())));

    doGet("/roles/capabilities")
      .andExpect(content().json(asJsonString(roleCapabilities(
        roleCapability(ROLE_ID, FOO_VIEW_CAPABILITY),
        roleCapability(ROLE_ID, FOO_CREATE_CAPABILITY)
      ))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-test-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void capabilityAssigmentFlow_positive() throws Exception {
    var req = roleCapabilitiesRequest(ROLE_ID, FOO_DELETE_CAPABILITY);
    var fooItemDeleteRoleCapability = roleCapability(ROLE_ID, FOO_DELETE_CAPABILITY);
    postRoleCapabilities(req).andExpect(content().json(asJsonString(roleCapabilities(fooItemDeleteRoleCapability))));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(kcPermissionName(fooItemDeleteEndpoint())));

    updateRoleCapabilitySets(capabilitySetsUpdateRequest(FOO_MANAGE_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemPostEndpoint())));

    updateRoleCapabilitySets(capabilitySetsUpdateRequest(FOO_EDIT_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())));

    updateRoleCapabilities(capabilitiesUpdateRequest(FOO_CREATE_CAPABILITY, FOO_VIEW_CAPABILITY));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint())));

    mockMvc.perform(delete("/roles/{id}/capability-sets", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint())));

    mockMvc.perform(delete("/roles/{id}/capabilities", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  static void updateRoleCapabilitySets(CapabilitySetsUpdateRequest request) throws Exception {
    mockMvc.perform(put("/roles/{id}/capability-sets", ROLE_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }

  static ResultActions postRoleCapabilitySets(RoleCapabilitySetsRequest request) throws Exception {
    return attemptToPostRoleCapabilitySets(request).andExpect(status().isCreated());
  }

  static ResultActions attemptToPostRoleCapabilitySets(RoleCapabilitySetsRequest request) throws Exception {
    return mockMvc.perform(post("/roles/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON));
  }

  protected static String kcPermissionName(Endpoint endpoint) {
    return String.format("%s access for role '%s' to '%s'", endpoint.getMethod(), ROLE_ID, endpoint.getPath());
  }
}
