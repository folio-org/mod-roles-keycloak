package org.folio.roles.it;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
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
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.UserCapabilitiesRequest;
import org.folio.roles.support.CapabilityUtils;
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
class UserCapabilityIT extends BaseIntegrationTest {

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
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void findCapabilities_positive() throws Exception {
    doGet(get("/users/capabilities")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(userCapabilities(
        userCapability(USER_ID, FOO_CREATE_CAPABILITY), userCapability(USER_ID, FOO_VIEW_CAPABILITY)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void findCapabilities_positive_offsetAndLimit() throws Exception {
    doGet(get("/users/capabilities")
      .param("offset", "1")
      .param("limit", "1")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        userCapabilities(2L, userCapability(USER_ID, FOO_VIEW_CAPABILITY)))
      ));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void findCapabilities_positive_cqlQuery() throws Exception {
    doGet(get("/users/capabilities")
      .param("query", "capabilityId==\"" + FOO_CREATE_CAPABILITY + "\"")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(
        userCapabilities(userCapability(USER_ID, FOO_CREATE_CAPABILITY)))
      ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void assignCapabilities_positive() throws Exception {
    var request = userCapabilitiesRequest(USER_ID, FOO_DELETE_CAPABILITY, FOO_EDIT_CAPABILITY);
    var fooItemDeleteUserCapability = userCapability(USER_ID, FOO_DELETE_CAPABILITY);
    var fooItemViewUserCapability = userCapability(USER_ID, FOO_EDIT_CAPABILITY);
    var expectedUserCapabilities = userCapabilities(fooItemDeleteUserCapability, fooItemViewUserCapability);

    postUserCapabilities(request)
      .andExpect(content().json(asJsonString(expectedUserCapabilities)))
      .andExpect(jsonPath("$.userCapabilities[0].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.userCapabilities[1].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  void assignCapabilities_positive_cleanInstallation() throws Exception {
    var request = userCapabilitiesRequest(USER_ID, FOO_CREATE_CAPABILITY, FOO_EDIT_CAPABILITY);
    var fooItemViewUserCapability = userCapability(USER_ID, FOO_CREATE_CAPABILITY);
    var fooItemEditUserCapability = userCapability(USER_ID, FOO_EDIT_CAPABILITY);
    var expectedUserCapabilities = userCapabilities(fooItemViewUserCapability, fooItemEditUserCapability);

    postUserCapabilities(request)
      .andExpect(content().json(asJsonString(expectedUserCapabilities)))
      .andExpect(jsonPath("$.userCapabilities[0].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.userCapabilities[1].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.userCapabilities[1].metadata.createdDate", notNullValue()));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint())));
  }

  @Test
  void assignCapabilities_negative_emptyCapabilities() throws Exception {
    var request = userCapabilitiesRequest(USER_ID, emptyList());
    attemptToPostUserCapabilities(request)
      .andExpect(status().isBadRequest())
      .andExpectAll(argumentNotValidErr("size must be between 1 and 255", "capabilityIds", "[]"));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void assignCapabilities_negative_alreadyAssigned() throws Exception {
    var capabilityIds = List.of(FOO_CREATE_CAPABILITY, FOO_VIEW_CAPABILITY);
    var request = userCapabilitiesRequest(USER_ID, capabilityIds);

    attemptToPostUserCapabilities(request)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message", is(String.format(
        "Relation already exists for user='%s' and capabilities=%s", USER_ID, capabilityIds))))
      .andExpect(jsonPath("$.errors[0].code", is("found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityExistsException")))
      .andExpect(jsonPath("$.total_records", is(1)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void findByUserId_positive() throws Exception {
    doGet(get("/users/{id}/capabilities", USER_ID)
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_CREATE_CAPABILITY, FOO_RESOURCE, CREATE, "foo.item.post", fooItemPostEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint())))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-many-user-capability-set-relations.sql"
  })
  void findByUserId_positive_expandCapabilitySets() throws Exception {
    doGet(get("/users/{id}/capabilities", USER_ID)
      .queryParam("expand", "true")
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_CREATE_CAPABILITY, FOO_RESOURCE, CREATE, "foo.item.post", fooItemPostEndpoint()),
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint())
      ))));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void update_positive() throws Exception {
    var request = CapabilityUtils.capabilitiesUpdateRequest(
      FOO_VIEW_CAPABILITY,
      FOO_EDIT_CAPABILITY,
      FOO_DELETE_CAPABILITY);

    updateUserCapabilities(request);

    doGet("/users/capabilities")
      .andExpect(content().json(asJsonString(userCapabilities(
        userCapability(USER_ID, FOO_VIEW_CAPABILITY),
        userCapability(USER_ID, FOO_EDIT_CAPABILITY),
        userCapability(USER_ID, FOO_DELETE_CAPABILITY)))));

    assertThat(kcTestClient.getPermissionNames()).containsAll(List.of(
      kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPutEndpoint())
    ));
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-user-capability-relations.sql"
  })
  void deleteCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/users/{id}/capabilities", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    doGet("/users/capabilities").andExpect(content().json(asJsonString(userCapabilities())));

    assertThat(kcTestClient.getPermissionNames()).isEmpty();
  }

  @Test
  @KeycloakRealms("/json/keycloak/user-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-user-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql"
  })
  void deleteCapabilities_negative_nothingToDelete() throws Exception {
    var expectedErrorMessage = "Relations between user and capabilities are not found for user: " + USER_ID;
    mockMvc.perform(delete("/users/{id}/capabilities", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(expectedErrorMessage)));
  }

  static void updateUserCapabilities(CapabilitiesUpdateRequest request) throws Exception {
    mockMvc.perform(put("/users/{id}/capabilities", USER_ID)
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }

  static ResultActions postUserCapabilities(UserCapabilitiesRequest request) throws Exception {
    return attemptToPostUserCapabilities(request).andExpect(status().isCreated());
  }

  static ResultActions attemptToPostUserCapabilities(UserCapabilitiesRequest request) throws Exception {
    return mockMvc.perform(post("/users/capabilities")
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON));
  }

  protected static String kcPermissionName(Endpoint endpoint) {
    return String.format("%s access for user 'test-user' to '%s'", endpoint.getMethod(), endpoint.getPath());
  }
}
