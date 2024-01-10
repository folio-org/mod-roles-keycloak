package org.folio.roles.it;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.it.UserCapabilityIT.postUserCapabilities;
import static org.folio.roles.it.UserCapabilityIT.updateUserCapabilities;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITY_SET;
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
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySet;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySets;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetsRequest;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilities;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilitiesRequest;
import static org.folio.roles.support.UserCapabilityUtils.userCapability;
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
import org.folio.roles.domain.dto.UserCapabilitySetsRequest;
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
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-user-capability-tables.sql"
})
class UserCapabilitySetIT extends BaseIntegrationTest {

  private static final UUID USER_ID = fromString("3e8647ee-2a23-4ca4-896b-95476559c567");

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
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-user-capability-set-relations.sql"
  })
  void findCapabilitySets_positive() throws Exception {
    doGet(get("/users/capability-sets")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(userCapabilitySets(
        userCapabilitySet(USER_ID, FOO_CREATE_CAPABILITY_SET)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-user-capability-set-relations.sql"
  })
  void findCapabilitySets_positive_offsetAndLimit() throws Exception {
    doGet(get("/users/capability-sets")
      .param("offset", "1")
      .param("limit", "1")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        userCapabilitySets(2L, userCapabilitySet(USER_ID, FOO_EDIT_CAPABILITY_SET)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-user-capability-set-relations.sql"
  })
  void findCapabilitySets_positive_cqlQuery() throws Exception {
    doGet(get("/users/capability-sets")
      .param("query", "capabilitySetId==\"" + FOO_CREATE_CAPABILITY_SET + "\"")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(userCapabilitySets(
        userCapabilitySet(USER_ID, FOO_CREATE_CAPABILITY_SET)))
      ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void assignCapabilitySets_positive() throws Exception {
    var request = userCapabilitySetsRequest(USER_ID, FOO_EDIT_CAPABILITY_SET, FOO_CREATE_CAPABILITY_SET);
    var fooItemEditCapabilitySet = userCapabilitySet(USER_ID, FOO_EDIT_CAPABILITY_SET);
    var fooItemCreateCapabilitySet = userCapabilitySet(USER_ID, FOO_CREATE_CAPABILITY_SET);
    var expectedUserCapabilitySets = userCapabilitySets(fooItemEditCapabilitySet, fooItemCreateCapabilitySet);

    postUserCapabilitySets(request)
      .andExpect(content().json(asJsonString(expectedUserCapabilitySets)))
      .andExpect(jsonPath("$.userCapabilitySets[0].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.userCapabilitySets[1].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilitySets[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemGetEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void assignCapabilities_positive_cleanInstallation() throws Exception {
    var request = userCapabilitySetsRequest(USER_ID, FOO_EDIT_CAPABILITY_SET, FOO_CREATE_CAPABILITY_SET);
    var fooItemEditCapabilitySet = userCapabilitySet(USER_ID, FOO_EDIT_CAPABILITY_SET);
    var fooItemCreateCapabilitySet = userCapabilitySet(USER_ID, FOO_CREATE_CAPABILITY_SET);
    var expectedUserCapabilitySets = userCapabilitySets(fooItemEditCapabilitySet, fooItemCreateCapabilitySet);

    postUserCapabilitySets(request)
      .andExpect(content().json(asJsonString(expectedUserCapabilitySets)))
      .andExpect(jsonPath("$.userCapabilitySets[0].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.userCapabilitySets[1].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilitySets[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint())
    ));
  }

  @Test
  void assignCapabilities_negative_emptyCapabilities() throws Exception {
    var request = userCapabilitySetsRequest(USER_ID, emptyList());
    attemptToPostUserCapabilitySets(request)
      .andExpect(status().isBadRequest())
      .andExpectAll(argumentNotValidErr("size must be between 1 and 255", "capabilitySetIds", "[]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-user-capability-set-relations.sql"
  })
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  void assignCapabilities_negative_alreadyAssigned() throws Exception {
    var capabilitySetIds = List.of(FOO_CREATE_CAPABILITY_SET);
    var request = userCapabilitySetsRequest(USER_ID, capabilitySetIds);

    attemptToPostUserCapabilitySets(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityExistsException")))
      .andExpect(jsonPath("$.errors[0].message", is(String.format(
        "Relation already exists for user='%s' and capabilitySets=%s", USER_ID, capabilitySetIds))))
    ;
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-user-capability-set-relations.sql"
  })
  void findByUserId_positive() throws Exception {
    var expectedEditCapabilityIds = List.of(FOO_VIEW_CAPABILITY, FOO_EDIT_CAPABILITY);
    var expectedCreateCapabilityIds = List.of(FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY);

    doGet(get("/users/{id}/capability-sets", USER_ID)
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilitySets(
        capabilitySet(FOO_EDIT_CAPABILITY_SET, FOO_RESOURCE, EDIT, expectedEditCapabilityIds),
        capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, expectedCreateCapabilityIds)
      ))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-user-capability-set-relations.sql"
  })
  void update_positive() throws Exception {
    var request = capabilitySetsUpdateRequest(FOO_EDIT_CAPABILITY_SET);
    updateUserCapabilitySets(request);

    var fooItemEditCapabilitySet = userCapabilitySet(USER_ID, FOO_EDIT_CAPABILITY_SET);
    doGet("/users/capability-sets")
      .andExpect(content().json(asJsonString(userCapabilitySets(fooItemEditCapabilitySet))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-user-capability-set-relations.sql"
  })
  void deleteCapabilitySets_positive() throws Exception {
    mockMvc.perform(delete("/users/{id}/capability-sets", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/users/capability-sets")
      .andExpect(content().json(asJsonString(userCapabilitySets())));

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void deleteCapabilitySets_negative_nothingToDelete() throws Exception {
    var expectedErrorMessage = "Relations between user and capability sets are not found for user: " + USER_ID;
    mockMvc.perform(delete("/users/{id}/capability-sets", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(expectedErrorMessage)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-user-capability-set-relations.sql"
  })
  void deleteCapabilitySets_positive_shouldKeepDirectlyAssignedCapabilities() throws Exception {
    mockMvc.perform(delete("/users/{id}/capability-sets", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/users/capability-sets")
      .andExpect(content().json(asJsonString(userCapabilitySets())));

    doGet("/users/capabilities")
      .andExpect(content().json(asJsonString(userCapabilities(
        userCapability(USER_ID, FOO_VIEW_CAPABILITY),
        userCapability(USER_ID, FOO_CREATE_CAPABILITY)
      ))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void capabilityAssigmentFlow_positive() throws Exception {
    var req = userCapabilitiesRequest(USER_ID, FOO_DELETE_CAPABILITY);
    var fooItemDeleteUserCapability = userCapability(USER_ID, FOO_DELETE_CAPABILITY);
    postUserCapabilities(req).andExpect(content().json(asJsonString(userCapabilities(fooItemDeleteUserCapability))));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(kcPermissionName(fooItemDeleteEndpoint())));

    updateUserCapabilitySets(capabilitySetsUpdateRequest(FOO_MANAGE_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemPostEndpoint())));

    updateUserCapabilitySets(capabilitySetsUpdateRequest(FOO_EDIT_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())));

    updateUserCapabilities(capabilitiesUpdateRequest(FOO_CREATE_CAPABILITY, FOO_VIEW_CAPABILITY));
    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemPostEndpoint())));

    mockMvc.perform(delete("/users/{id}/capability-sets", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint())));

    mockMvc.perform(delete("/users/{id}/capabilities", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  static void updateUserCapabilitySets(CapabilitySetsUpdateRequest request) throws Exception {
    mockMvc.perform(put("/users/{id}/capability-sets", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }

  static ResultActions postUserCapabilitySets(UserCapabilitySetsRequest request) throws Exception {
    return attemptToPostUserCapabilitySets(request).andExpect(status().isCreated());
  }

  static ResultActions attemptToPostUserCapabilitySets(UserCapabilitySetsRequest request) throws Exception {
    return mockMvc.perform(post("/users/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON));
  }

  protected static String kcPermissionName(Endpoint endpoint) {
    return String.format("%s access for user '%s' to '%s'", endpoint.getMethod(), USER_ID, endpoint.getPath());
  }
}
