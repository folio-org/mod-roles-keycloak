package org.folio.roles.it;

import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetsRequest;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilitiesRequest;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.support.TestUtils.awaitUntilAsserted;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Durations.TWO_SECONDS;

import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.KeycloakTestClient;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Log4j2
@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-permission-table.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql",
})
class RoleCapabilitySetUpdateIT extends BaseIntegrationTest {

  private static final UUID ROLE_ID = fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e");

  @Autowired private KeycloakTestClient kcTestClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
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
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapabilitySet() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-set-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var request = getFooCreateCapabilitySet();
    awaitUntilAsserted(() -> doGet(request).andExpect(content().json(asJsonString(capabilitySets()))));
    awaitUntilAsserted(RoleCapabilitySetUpdateIT::assertAssignedCapabilitySets);
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).isEmpty());
    doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(9)));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapabilitySetPermissionsMissing() {
    var capabilityEvent = readValue("json/kafka-events/be-capability-set-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var request = getFooCreateCapabilitySet();
    awaitUntilAsserted(() -> doGet(request).andExpect(content().json(asJsonString(capabilitySets()))));
    awaitUntilAsserted(RoleCapabilitySetUpdateIT::assertAssignedCapabilitySets);
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).isEmpty());
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapabilitySetWithAssignedCapability() throws Exception {
    postRoleCapabilities(roleCapabilitiesRequest(ROLE_ID, FOO_VIEW_CAPABILITY));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-set-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var request = getFooCreateCapabilitySet();
    awaitUntilAsserted(() -> doGet(request).andExpect(content().json(asJsonString(capabilitySets()))));
    awaitUntilAsserted(RoleCapabilitySetUpdateIT::assertAssignedCapabilitySets);
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames())
      .containsExactlyInAnyOrder(kcPermissionName(fooItemGetEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapabilitySetWithAssignedCapabilitySet() throws Exception {
    postRoleCapabilitySets(roleCapabilitySetsRequest(ROLE_ID, FOO_MANAGE_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemDeleteEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-set-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var request = getFooCreateCapabilitySet();
    awaitUntilAsserted(() -> doGet(request).andExpect(content().json(asJsonString(capabilitySets()))));

    var capabilitySet = capabilitySet(FOO_MANAGE_CAPABILITY_SET, FOO_RESOURCE, MANAGE, FOO_MANAGE_CAPABILITIES);
    awaitUntilAsserted(() -> assertAssignedCapabilitySets(capabilitySet));
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemDeleteEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/permissions/populate-permissions.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_updatedCapability() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-set-upgrade-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var newCapabilities = List.of(FOO_VIEW_CAPABILITY, FOO_DELETE_CAPABILITY, FOO_CREATE_CAPABILITY);
    var updatedCapabilitySet = capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, newCapabilities)
      .permission("foo.item.create")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> doGet(getFooCreateCapabilitySet())
      .andExpect(content().json(asJsonString(capabilitySets(updatedCapabilitySet)))));

    assertAssignedCapabilitySets(updatedCapabilitySet);
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemDeleteEndpoint()),
      kcPermissionName(fooItemPostEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/permissions/populate-permissions.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_updatedCapabilitySetWithAssignedCapability() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-set-upgrade-event.json", ResourceEvent.class);
    postRoleCapabilities(roleCapabilitiesRequest(ROLE_ID, FOO_CREATE_CAPABILITY));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()));

    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var newCapabilities = List.of(FOO_VIEW_CAPABILITY, FOO_DELETE_CAPABILITY, FOO_CREATE_CAPABILITY);
    var updatedCapabilitySet = capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, newCapabilities)
      .permission("foo.item.create")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> doGet(getFooCreateCapabilitySet())
      .andExpect(content().json(asJsonString(capabilitySets(updatedCapabilitySet)))));

    assertAssignedCapabilitySets(updatedCapabilitySet);
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/permissions/populate-permissions.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capability-sets/populate-role-capability-set-relations.sql"
  })
  void handleCapabilityEvent_positive_updatedCapabilitySetWithAssignedCapabilitySet() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-set-upgrade-event.json", ResourceEvent.class);
    postRoleCapabilitySets(roleCapabilitySetsRequest(ROLE_ID, FOO_MANAGE_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemPutEndpoint()));

    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var newCapabilities = List.of(FOO_VIEW_CAPABILITY, FOO_DELETE_CAPABILITY, FOO_CREATE_CAPABILITY);
    var updatedCapabilitySet = capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, newCapabilities)
      .permission("foo.item.create")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> doGet(getFooCreateCapabilitySet())
      .andExpect(content().json(asJsonString(capabilitySets(updatedCapabilitySet)))));

    var capabilitySet = capabilitySet(FOO_MANAGE_CAPABILITY_SET, FOO_RESOURCE, MANAGE, FOO_MANAGE_CAPABILITIES);
    assertAssignedCapabilitySets(updatedCapabilitySet, capabilitySet);
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemPutEndpoint())));
  }

  private static MockHttpServletRequestBuilder getFooCreateCapabilitySet() {
    return get("/capability-sets")
      .queryParam("query", "permission==foo.item.create")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(okapiHeaders());
  }

  private static void assertAssignedCapabilitySets(CapabilitySet... capabilities) throws Exception {
    doGet(get("/roles/{roleId}/capability-sets", ROLE_ID)
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilitySets(capabilities))));
  }

  static void postRoleCapabilities(RoleCapabilitiesRequest request) throws Exception {
    mockMvc.perform(post("/roles/capabilities")
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

  static void postRoleCapabilitySets(RoleCapabilitySetsRequest request) throws Exception {
    mockMvc.perform(post("/roles/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)
        .content(asJsonString(request))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

  protected static String kcPermissionName(Endpoint endpoint) {
    return String.format("%s access for role '%s' to '%s'", endpoint.getMethod(), ROLE_ID, endpoint.getPath());
  }
}
