package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.DELETE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.EXECUTE;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.domain.dto.CapabilityType.PROCEDURAL;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPatchEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.testcontainers.shaded.org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.testcontainers.shaded.org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;

import java.sql.SQLDataException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.CapabilityType;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.integration.kafka.CapabilitySetDescriptorService;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.test.types.IntegrationTest;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionFactory;

@Log4j2
@IntegrationTest
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-permission-table.sql",
  "classpath:/sql/truncate-capability-tables.sql",
})
class KafkaMessageListenerIT extends BaseIntegrationTest {

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @SpyBean private CapabilityService capabilityService;
  @SpyBean private CapabilitySetDescriptorService capabilitySetDescriptorService;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  void handleCapabilityEvent_positive_freshInstallation() throws Exception {
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(0))));
    sendCapabilityEventAndCheckResult();
  }

  @Test
  @Sql("classpath:/sql/kafka-message-listener-it/all-capabilities.sql")
  void handleCapabilityEvent_positive_prePopulatedCapabilities() throws Exception {
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
    sendCapabilityEventAndCheckResult();
  }

  @Test
  @Sql("classpath:/sql/kafka-message-listener-it/partial-capabilities.sql")
  void handleCapabilityEvent_positive_partiallyPopulatedCapabilities() throws Exception {
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(3))));
    sendCapabilityEventAndCheckResult();
  }

  @Test
  @Sql("classpath:/sql/kafka-message-listener-it/all-capabilities.sql")
  void handleCapabilityEvent_positive_applicationVersionUpgradeEvent() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-app-upgrade-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var expectedCapabilitiesJson = asJsonString(capabilities(
      fooItemCapability(MANAGE, "foo.item.all", APPLICATION_ID_V2),
      fooItemCapability(CREATE, "foo.item.post", APPLICATION_ID_V2, fooItemPostEndpoint()),
      fooItemCapability(DELETE, "foo.item.delete", APPLICATION_ID_V2, fooItemDeleteEndpoint()),
      fooItemCapability(EDIT, "foo.item.put", APPLICATION_ID_V2, fooItemPutEndpoint(), fooItemPatchEndpoint()),
      fooItemCapability(VIEW, "foo.item.get", APPLICATION_ID_V2, fooItemGetEndpoint())));

    var expectedCapabilitySets = asJsonString(capabilitySets(
      fooItemCapabilitySet(CREATE, APPLICATION_ID_V2),
      fooItemCapabilitySet(EDIT, APPLICATION_ID_V2),
      fooItemCapabilitySet(MANAGE, APPLICATION_ID_V2),
      fooItemCapabilitySet(VIEW, APPLICATION_ID_V2)));

    await().untilAsserted(() -> doGet("/capabilities").andExpect(content().json(expectedCapabilitiesJson)));
    await().untilAsserted(() -> doGet("/capability-sets").andExpect(content().json(expectedCapabilitySets)));

    var capabilitiesResponse = parseResponse(doGet("/capability-sets").andReturn(), CapabilitySets.class);
    var capabilitySets = capabilitiesResponse.getCapabilitySets();

    assertCapabilityNamesBySetId(capabilitySets.get(0).getId(), "foo_item.create", "foo_item.edit", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(1).getId(), "foo_item.edit", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(2).getId(),
      "foo_item.create", "foo_item.delete", "foo_item.edit", "foo_item.manage", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(3).getId(), "foo_item.view");
  }

  @Test
  void handleCapabilityEvent_positive_uiPermissions() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var uiCapabilityEvent = readValue("json/kafka-events/ui-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, uiCapabilityEvent);

    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(9))));
    await().untilAsserted(() -> doGet("/capability-sets").andExpect(jsonPath("$.totalRecords", is(7))));

    var searchResult = doGet(get("/capability-sets")
      .queryParam("query", "name == \"ui-test_foo.create\"")
      .headers(okapiHeaders()))
      .andExpect(jsonPath("$.totalRecords", is(1)))
      .andReturn();

    var foundCapabilitySet = parseResponse(searchResult, CapabilitySets.class).getCapabilitySets().get(0);
    assertThat(foundCapabilitySet.getCapabilities()).hasSize(7);

    doGet("/capability-sets/{id}/capabilities", foundCapabilitySet.getId())
      .andExpect(jsonPath("$.capabilities[*].name", containsInAnyOrder("foo_item.create", "foo_item.edit",
        "foo_item.view", "settings_enabled.view", "settings_test_enabled.view", "ui-test_foo.view",
        "ui-test_foo.create")));
  }

  @Test
  void handleCapabilityEvent_positive_permissionReplacesSameOverridePermission() {
    var capabilityEvent =
      readValue("json/kafka-events/be-capability-event-replaced-perm-is-override.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var expectedCapabilitiesJson = asJsonString(capabilities(
      fooItemCapability(EXECUTE, PROCEDURAL, "foo.item.execute")));

    await().untilAsserted(() -> doGet("/capabilities")
      .andExpect(content().json(expectedCapabilitiesJson))
      .andExpect(jsonPath("$.capabilities[0].metadata.createdDate", notNullValue())));
  }

  @Test
  void handleCapabilityEvent_positive_sameCapabilityNameCreatedByDifferentPermissions() {
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(0))));
    var capabilityEvent =
      readValue("json/kafka-events/be-capability-event-replaces-contains-same-capability-by-permission.json",
        ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var expectedCapabilitiesJson = asJsonString(capabilities(
      fooItemCapability(CREATE, DATA, "foo.item.create"),
      fooItemCapability(EDIT, DATA, "foo.item.update")));

    await().untilAsserted(() -> doGet("/capabilities")
      .andExpect(content().json(expectedCapabilitiesJson))
      .andExpect(jsonPath("$.capabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilities[1].metadata.createdDate", notNullValue())));
  }

  private void sendCapabilityEventAndCheckResult() throws Exception {
    var capabilityEvent = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);

    var expectedCapabilitiesJson = asJsonString(capabilities(
      fooItemCapability(MANAGE, "foo.item.all"),
      fooItemCapability(CREATE, "foo.item.post", fooItemPostEndpoint()),
      fooItemCapability(DELETE, "foo.item.delete", fooItemDeleteEndpoint()),
      fooItemCapability(EDIT, "foo.item.put", fooItemPutEndpoint(), fooItemPatchEndpoint()),
      fooItemCapability(VIEW, "foo.item.get", fooItemGetEndpoint())));

    await().untilAsserted(() -> doGet("/capabilities")
      .andExpect(content().json(expectedCapabilitiesJson))
      .andExpect(jsonPath("$.capabilities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilities[1].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilities[2].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilities[3].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilities[4].metadata.createdDate", notNullValue())));

    var expectedCapabilitySets = asJsonString(capabilitySets(
      fooItemCapabilitySet(CREATE),
      fooItemCapabilitySet(EDIT),
      fooItemCapabilitySet(MANAGE),
      fooItemCapabilitySet(VIEW)));

    await().untilAsserted(() -> doGet("/capability-sets")
      .andExpect(content().json(expectedCapabilitySets))
      .andExpect(jsonPath("$.capabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilitySets[1].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilitySets[2].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilitySets[3].metadata.createdDate", notNullValue())));

    var capabilitiesResponse = parseResponse(doGet("/capability-sets").andReturn(), CapabilitySets.class);
    var capabilitySets = capabilitiesResponse.getCapabilitySets();

    assertCapabilityNamesBySetId(capabilitySets.get(0).getId(), "foo_item.create", "foo_item.edit", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(1).getId(), "foo_item.edit", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(2).getId(),
      "foo_item.create", "foo_item.delete", "foo_item.edit", "foo_item.manage", "foo_item.view");
    assertCapabilityNamesBySetId(capabilitySets.get(3).getId(), "foo_item.view");
  }

  @Test
  void handleCapabilityEvent_positive_eventIsSentWhenTenantIsDisabled() {
    var capabilityEvent = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
    await().untilAsserted(() -> doGet("/capability-sets").andExpect(jsonPath("$.totalRecords", is(4))));

    removeTenant(TENANT_ID);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    awaitFor(FIVE_HUNDRED_MILLISECONDS);

    enableTenant(TENANT_ID);
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
    await().untilAsserted(() -> doGet("/capability-sets").andExpect(jsonPath("$.totalRecords", is(4))));
  }

  @MethodSource("exceptionDataProvider")
  @ParameterizedTest(name = "[{index}] name={0}")
  @DisplayName("handleCapabilityEvent_negative_parameterizedForNonRetryableExceptions")
  void handleCapabilityEvent_negative_parameterized(@SuppressWarnings("unused") String name, Throwable throwable) {
    var capabilityEvent = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
    doThrow(throwable).when(capabilityService).update(eq(ResourceEventType.CREATE), anyList(), anyList());

    awaitFor(FIVE_HUNDRED_MILLISECONDS);

    verify(capabilitySetDescriptorService, times(0)).update(any(), any(), any());
  }

  private static Stream<Arguments> exceptionDataProvider() {
    return Stream.of(
      arguments("RuntimeException", new RuntimeException("error")),

      arguments("InvalidDataAccessResourceUsageException without cause",
        new InvalidDataAccessResourceUsageException("invalid data access error")),

      arguments("InvalidDataAccessResourceUsageException with RuntimeException cause",
        new InvalidDataAccessResourceUsageException("invalid data access error", new RuntimeException("error"))),

      arguments("InvalidDataAccessResourceUsageException with SQLGrammarExceptionCause",
        new InvalidDataAccessResourceUsageException("invalid data access error",
          new SQLGrammarException("sql grammar error", null))),

      arguments("InvalidDataAccessResourceUsageException with SQLGrammarExceptionCause, but cause is not PSQLException",
        new InvalidDataAccessResourceUsageException("invalid data access error",
          new SQLGrammarException("sql grammar error", new SQLDataException()))),

      arguments("InvalidDataAccessResourceUsageException with PSQLException cause",
        new InvalidDataAccessResourceUsageException("invalid data access error",
          new SQLGrammarException("sql grammar error", new PSQLException("unknown", null)))),

      arguments("InvalidDataAccessResourceUsageException with PSQLException cause, but non-retryable message 1",
        new InvalidDataAccessResourceUsageException("invalid data access error",
          new SQLGrammarException("sql grammar error", new PSQLException("ERROR: relation is invalid", null)))),

      arguments("InvalidDataAccessResourceUsageException with PSQLException cause, but non-retryable message 1",
        new InvalidDataAccessResourceUsageException("invalid data access error",
          new SQLGrammarException("sql grammar error", new PSQLException("'capability' table does not exist", null))))
    );
  }

  /**
   * Sonar friendly Thread.sleep(millis) implementation
   *
   * @param duration - duration to await.
   */
  @SuppressWarnings("SameParameterValue")
  private static void awaitFor(Duration duration) {
    var sampleResult = Optional.of(1);
    Awaitility.await()
      .pollInSameThread()
      .atMost(duration.plus(Duration.ofMillis(250)))
      .pollDelay(duration)
      .untilAsserted(() -> assertThat(sampleResult).isPresent());
  }

  private static void assertCapabilityNamesBySetId(UUID capabilitySetId, String... expectedNames) throws Exception {
    doGet("/capability-sets/{id}/capabilities", capabilitySetId)
      .andExpect(jsonPath("$.capabilities[*].name", is(Arrays.asList(expectedNames))));
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(TEN_SECONDS).pollInterval(ONE_HUNDRED_MILLISECONDS);
  }

  private static Capability fooItemCapability(CapabilityAction action, String permission, Endpoint... endpoints) {
    return fooItemCapability(action, DATA, permission, APPLICATION_ID, endpoints);
  }

  private static Capability fooItemCapability(CapabilityAction action, CapabilityType type, String permission,
    Endpoint... endpoints) {
    return fooItemCapability(action, type, permission, APPLICATION_ID, endpoints);
  }

  private static Capability fooItemCapability(CapabilityAction action, String permission,
    String applicationId, Endpoint... endpoints) {
    return fooItemCapability(action, DATA, permission, applicationId, endpoints);
  }

  private static Capability fooItemCapability(CapabilityAction action, CapabilityType type, String permission,
    String applicationId, Endpoint... endpoints) {
    var capabilityName = getCapabilityName(FOO_RESOURCE, action);
    return new Capability()
      .id(null)
      .name(capabilityName)
      .resource(FOO_RESOURCE)
      .action(action)
      .type(type)
      .applicationId(applicationId)
      .permission(permission)
      .endpoints(Arrays.asList(endpoints))
      .description(capabilityName + " - description");
  }

  private static CapabilitySet fooItemCapabilitySet(CapabilityAction action) {
    return fooItemCapabilitySet(action, APPLICATION_ID);
  }

  private static CapabilitySet fooItemCapabilitySet(CapabilityAction action, String applicationId) {
    var capabilityName = getCapabilityName(FOO_RESOURCE, action);
    return new CapabilitySet()
      .name(capabilityName)
      .action(action)
      .applicationId(applicationId)
      .resource(FOO_RESOURCE)
      .type(DATA)
      .description(capabilityName + " - description");
  }
}
