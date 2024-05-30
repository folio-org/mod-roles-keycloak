package org.folio.roles.it;

import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_V2_CAPABILITY_SET;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_MANAGE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.fooItemCapability;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetCollectionEndpoint;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.KeycloakTestClient;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
class RoleCapabilityUpdateIT extends BaseIntegrationTest {

  private static final UUID ROLE_ID = fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e");

  @Autowired private KeycloakTestClient kcTestClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapability() throws Exception {
    var deprecatedCapability = fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get", fooItemGetEndpoint());
    var unmodifiedCapability = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post", fooItemPostEndpoint());
    assertAssignedCapabilities(deprecatedCapability, unmodifiedCapability);

    var capabilityEvent = readValue("json/kafka-events/be-capability-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities()))));

    doGet(getCapabilityByPermission("foo.item.post")).andExpect(
      content().json(asJsonString(capabilities(unmodifiedCapability))));

    var expectedPermissionName = kcPermissionName(fooItemPostEndpoint());
    awaitUntilAsserted(() -> assertAssignedCapabilities(unmodifiedCapability));
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).containsExactly(expectedPermissionName));
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
  void handleCapabilityEvent_positive_deprecatedCapabilityInCapabilitySet() {
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities()))));

    var unmodifiedCapability = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post", fooItemPostEndpoint());
    var expectedPermissionName = kcPermissionName(fooItemPostEndpoint());
    awaitUntilAsserted(() -> assertAssignedCapabilities(true, unmodifiedCapability));
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).containsExactly(expectedPermissionName));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void handleCapabilityEvent_positive_deprecatedCapabilityInCapabilitySets() throws Exception {
    postRoleCapabilitySets(roleCapabilitySetsRequest(ROLE_ID, FOO_CREATE_CAPABILITY_SET, FOO_MANAGE_V2_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemDeleteEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var unmodifiedCapability = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post", fooItemPostEndpoint());
    var complexCapability = fooItemCapability(FOO_MANAGE_CAPABILITY, MANAGE, "foo.item.all")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemPostEndpoint(), fooItemPutEndpoint(), fooItemDeleteEndpoint()));

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities()))));

    awaitUntilAsserted(() -> assertAssignedCapabilities(true, unmodifiedCapability, complexCapability));
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()),
      kcPermissionName(fooItemPutEndpoint()), kcPermissionName(fooItemDeleteEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void handleCapabilityEvent_positive_permissionNotFoundForDeprecatedEvent() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities()))));

    var unmodifiedCapability = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post", fooItemPostEndpoint());
    doGet(getCapabilityByPermission("foo.item.post"))
      .andExpect(content().json(asJsonString(capabilities(unmodifiedCapability))));

    assertAssignedCapabilities(unmodifiedCapability);
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames()).isEmpty());
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql"
  })
  void handleCapabilityEvent_positive_deprecatedCapabilityWithinComplexCapability() throws Exception {
    postRoleCapabilities(roleCapabilitiesRequest(ROLE_ID, FOO_VIEW_CAPABILITY, FOO_MANAGE_CAPABILITY));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-deprecated-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var complexCapability = fooItemCapability(FOO_MANAGE_CAPABILITY, MANAGE, "foo.item.all")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemPostEndpoint(), fooItemPutEndpoint(), fooItemDeleteEndpoint()));

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities()))));

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.all"))
      .andExpect(content().json(asJsonString(capabilities(complexCapability)))));

    awaitUntilAsserted(() -> assertAssignedCapabilities(complexCapability));
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/populate-role-policy.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capabilities/populate-role-capability-relations.sql"
  })
  void handleCapabilityEvent_positive_updatedCapability() {
    var capabilityEvent = readValue("json/kafka-events/be-capability-upgrade-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    var updatedCapability1 = fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemGetCollectionEndpoint()))
      .applicationId(APPLICATION_ID_V2);

    var updatedCapability2 = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities(updatedCapability1)))));
    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.post"))
      .andExpect(content().json(asJsonString(capabilities(updatedCapability2)))));

    awaitUntilAsserted(() -> assertAssignedCapabilities(updatedCapability1, updatedCapability2));
    awaitUntilAsserted(() -> assertThat(kcTestClient.getPermissionNames())
      .containsExactly(kcPermissionName(fooItemGetCollectionEndpoint()), kcPermissionName(fooItemGetEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql"
  })
  void handleCapabilityEvent_positive_updatedCapabilityWithinComplexCapability() throws Exception {
    var request = roleCapabilitiesRequest(ROLE_ID, FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY, FOO_MANAGE_CAPABILITY);
    postRoleCapabilities(request);
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-upgrade-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var updatedCapability1 = fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemGetCollectionEndpoint()))
      .applicationId(APPLICATION_ID_V2);

    var updatedCapability2 = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post")
      .applicationId(APPLICATION_ID_V2);

    var complexCapability = fooItemCapability(FOO_MANAGE_CAPABILITY, MANAGE, "foo.item.all")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemPostEndpoint(), fooItemPutEndpoint(), fooItemDeleteEndpoint()));

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.get"))
      .andExpect(content().json(asJsonString(capabilities(updatedCapability1)))));

    awaitUntilAsserted(() -> doGet(getCapabilityByPermission("foo.item.post"))
      .andExpect(content().json(asJsonString(capabilities(updatedCapability2)))));

    awaitUntilAsserted(() -> assertAssignedCapabilities(updatedCapability2, complexCapability, updatedCapability1));
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
        kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
        kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint()),
        kcPermissionName(fooItemGetCollectionEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void handleCapabilityEvent_positive_updatedCapabilityWithAssignedCapabilitySet() throws Exception {
    postRoleCapabilities(roleCapabilitiesRequest(ROLE_ID, FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY));
    postRoleCapabilitySets(roleCapabilitySetsRequest(ROLE_ID, FOO_MANAGE_V2_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-upgrade-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var updatedCapability1 = fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemGetCollectionEndpoint()))
      .applicationId(APPLICATION_ID_V2);

    var updatedCapability2 = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> assertAssignedCapabilities(updatedCapability2, updatedCapability1));
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemDeleteEndpoint()), kcPermissionName(fooItemGetEndpoint()),
      kcPermissionName(fooItemPostEndpoint()), kcPermissionName(fooItemPutEndpoint()),
      kcPermissionName(fooItemGetCollectionEndpoint())));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-capability-fresh-realm.json")
  @Sql(scripts = {
    "classpath:/sql/populate-role.sql",
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql"
  })
  void handleCapabilityEvent_positive_updatedCapabilityInCapabilitySet() throws Exception {
    postRoleCapabilitySets(roleCapabilitySetsRequest(ROLE_ID, FOO_CREATE_CAPABILITY_SET));
    assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemPostEndpoint()));

    var capabilityEvent = readValue("json/kafka-events/be-capability-upgrade-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var updatedCapability1 = fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get")
      .endpoints(List.of(fooItemGetEndpoint(), fooItemGetCollectionEndpoint()))
      .applicationId(APPLICATION_ID_V2);

    var updatedCapability2 = fooItemCapability(FOO_CREATE_CAPABILITY, CREATE, "foo.item.post")
      .applicationId(APPLICATION_ID_V2);

    awaitUntilAsserted(() -> assertAssignedCapabilities(true, updatedCapability2, updatedCapability1));
    awaitUntilAsserted(TWO_SECONDS, () -> assertThat(kcTestClient.getPermissionNames()).containsExactlyInAnyOrder(
      kcPermissionName(fooItemGetEndpoint()), kcPermissionName(fooItemGetCollectionEndpoint())));
  }

  private static MockHttpServletRequestBuilder getCapabilityByPermission(String permissionName) {
    return get("/capabilities")
      .queryParam("query", "permission==" + permissionName)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(okapiHeaders());
  }

  private static void assertAssignedCapabilities(Capability... capabilities) throws Exception {
    assertAssignedCapabilities(false, capabilities);
  }

  private static void assertAssignedCapabilities(boolean expand, Capability... capabilities) throws Exception {
    doGet(get("/roles/{roleId}/capabilities", ROLE_ID)
      .queryParam("expand", Boolean.toString(expand))
      .header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER))
      .andExpect(content().json(asJsonString(capabilities(capabilities))));
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
