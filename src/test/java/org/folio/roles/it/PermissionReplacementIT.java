package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilitiesRequest;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetsRequest;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.testcontainers.shaded.org.awaitility.Durations.ONE_MINUTE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql"})
public class PermissionReplacementIT extends BaseIntegrationTest {

  public static final String TEST_KC_CLIENT_ID = "00000000-0000-0000-0000-000000000010";
  public static final String TEST_KC_REALM_NAME = "test";

  @Autowired private Keycloak keycloak;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private ObjectMapper objectMapper;

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

  @DynamicPropertySource
  static void configureFeignClientUrl(DynamicPropertyRegistry registry) {
    registry.add("moduserskc.url", () -> wmAdminClient.getWireMockUrl());
    registry.add("folio.okapiUrl", () -> wmAdminClient.getWireMockUrl());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  @WireMockStub(scripts = {"/wiremock/stubs/moduserskc/ensure-kc-user.json"})
  void permissionReplacement_positive() throws Exception {
    // Create Keycloak resources
    createResource("/foo/items/{id}", "GET", "PUT", "POST", "DELETE", "PATCH");
    createResource("/foo/items", "GET", "PUT", "POST", "DELETE", "PATCH");

    // Create some capabilities and capabilitysets
    var capabilityEvent = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS)
      .untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS)
      .untilAsserted(() -> doGet("/capability-sets").andExpect(jsonPath("$.totalRecords", is(4))));

    // Verify that capabilities and capabilitysets were created
    var capability = objectMapper.readValue(doGet("/capabilities").andReturn().getResponse().getContentAsByteArray(),
        Capabilities.class).getCapabilities().stream().filter(cap -> cap.getName().equals("foo_item.view")).findAny()
      .get();
    var capabilitySet =
      objectMapper.readValue(doGet("/capability-sets").andReturn().getResponse().getContentAsByteArray(),
          CapabilitySets.class).getCapabilitySets().stream().filter(cap -> cap.getName().equals("foo_item.manage"))
        .findAny().get();

    // Create a Keycloak role
    var roleToCreate = new Role().name("test role").description("test description").type(RoleType.CONSORTIUM);
    var role = objectMapper.readValue(mockMvc.perform(
        post("/roles").content(asJsonString(roleToCreate)).header(TENANT, TENANT_ID)
          .header(USER_ID, USER_ID_HEADER).contentType(APPLICATION_JSON))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
      Role.class);

    // Assign capability to a role
    mockMvc.perform(post("/roles/capabilities").header(TENANT, TENANT_ID).header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(roleCapabilitiesRequest(role.getId(), capability.getId()))).contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON)).andExpect(status().isCreated());

    // Verify that role got the capability assigned to it
    doGet(get("/roles/" + role.getId() + "/capabilities").header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)).andExpect(
      jsonPath("$.capabilities[0].id", is(capability.getId().toString())));

    // Find out Keycloak test-user's FOLIO ID
    var userId = UUID.fromString(
      keycloak.realm("test").users().searchByUsername("test-user", true).stream().findAny().get().getAttributes()
        .get(KeycloakUserService.USER_ID_ATTR).stream().findFirst().get());

    // Assign capabilityset to a user test-user
    mockMvc.perform(post("/users/capability-sets").header(TENANT, TENANT_ID).header(USER_ID, USER_ID_HEADER)
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .content(asJsonString(userCapabilitySetsRequest(userId, capabilitySet.getId()))).contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON)).andExpect(status().isCreated());

    // Verify that user got the capabilityset assigned to it
    doGet(get("/users/" + userId + "/capability-sets").header(TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)).andExpect(
      jsonPath("$.capabilitySets[0].id", is(capabilitySet.getId().toString())));

    // Trigger replacement of capabilities/capabilitysets
    var capabilitiesReplacementEvent =
      readValue("json/kafka-events/be-capabilities-replacement-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilitiesReplacementEvent);

    // Verify that capability and capability set were replaced
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).untilAsserted(
      () -> doGet("/capabilities").andExpect(jsonPath("$.capabilities[*].name", hasItem("newfoo_item.view"))));
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).untilAsserted(
      () -> doGet("/capability-sets").andExpect(jsonPath("$.capabilitySets[*].name", hasItem("newfoo_item.manage"))));

    // Figure out IDs of newly created capability and capabilityset
    var newCapability = objectMapper.readValue(doGet("/capabilities").andReturn().getResponse().getContentAsByteArray(),
        Capabilities.class).getCapabilities().stream().filter(cap -> cap.getName().equals("newfoo_item.view")).findAny()
      .get();
    var newCapabilitySet =
      objectMapper.readValue(doGet("/capability-sets").andReturn().getResponse().getContentAsByteArray(),
          CapabilitySets.class).getCapabilitySets().stream().filter(cap -> cap.getName().equals("newfoo_item.manage"))
        .findAny().get();

    // Verify that user and role have their new capabilities/capabilitysets assigned - and
    // old capabilities/capabilitysets were removed
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> doGet(
      get("/roles/" + role.getId() + "/capabilities").header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)).andExpect(jsonPath("$.capabilities", hasSize(1)))
      .andExpect(jsonPath("$.capabilities[0].id", is(newCapability.getId().toString()))).andReturn().getResponse()
      .getContentAsString());
    await().atMost(ONE_MINUTE).pollInterval(ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> doGet(
      get("/users/" + userId + "/capability-sets").header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_HEADER)).andExpect(jsonPath("$.capabilitySets", hasSize(1)))
      .andExpect(jsonPath("$.capabilitySets[0].id", is(newCapabilitySet.getId().toString()))));

    // Verify that replaced capability and capability set were removed and are no longer present
    doGet("/capabilities").andExpect(jsonPath("$.capabilities[*].name", not(hasItem("foo_item.view"))));
    doGet("/capability-sets").andExpect(jsonPath("$.capabilitySets[*].name", not(hasItem("foo_item.manage"))));
  }

  protected void createResource(String resourceName, String... scopeNames) {
    var resource = new ResourceRepresentation();
    resource.setName(resourceName);
    for (String scopeName : scopeNames) {
      var scope = new ScopeRepresentation();
      scope.setName(scopeName);
      resource.addScope(scope);
    }
    var response =
      keycloak.realm(TEST_KC_REALM_NAME).clients().get(TEST_KC_CLIENT_ID).authorization().resources().create(resource);
    assertThat(response.getStatus()).isEqualTo(201);
  }
}
