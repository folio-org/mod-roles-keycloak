package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.FakeKafkaConsumer.getEvents;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.integration.kafka.model.ResourceResultStatus;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.integration.kafka.CapabilityKafkaEventHandler;
import org.folio.test.FakeKafkaConsumer;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
@TestPropertySource(properties = "application.event-confirmation.enabled=true")
class KafkaEventConfirmationIT extends BaseIntegrationTest {

  private static final String CONFIRMATION_TOPIC = "it-test.mgr-tenant-entitlements.resource-result";
  private static final String RESOURCE_NAME = "Capability";
  private static final String MODULE_ID = "foo-module-1.0.0";

  @MockitoSpyBean private CapabilityKafkaEventHandler capabilityKafkaEventHandler;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeAll
  static void beforeAll(@Autowired FakeKafkaConsumer fakeKafkaConsumer) {
    enableTenant(TENANT_ID);
    fakeKafkaConsumer.registerTopic(CONFIRMATION_TOPIC, ResourceResultEvent.class);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @BeforeEach
  void beforeEach() {
    FakeKafkaConsumer.removeAllEvents();
  }

  @Test
  void handleCapabilityEvent_positive_publishesSuccessConfirmation() {
    doReturn(Optional.empty()).when(capabilityKafkaEventHandler).handleEvent(any());

    var event = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, event);

    awaitConfirmation(TENANT_ID, RESOURCE_NAME, MODULE_ID, SUCCESS, false);
  }

  @Test
  void handleCapabilityEvent_negative_publishesFailureConfirmation() {
    doThrow(new RuntimeException("capability event processing failed"))
      .when(capabilityKafkaEventHandler).handleEvent(any());

    var event = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, event);

    awaitConfirmation(TENANT_ID, RESOURCE_NAME, MODULE_ID, FAILURE, true);
  }

  private static void awaitConfirmation(String tenant, String resourceName,
    String moduleId, ResourceResultStatus status, boolean withDetails) {
    await().atMost(FIVE_SECONDS).untilAsserted(() -> {
      var events = getEvents(CONFIRMATION_TOPIC, ResourceResultEvent.class);
      assertThat(events).hasSize(1);
      var c = events.getFirst().value();
      assertThat(c.getTenant()).isEqualTo(tenant);
      assertThat(c.getResourceName()).isEqualTo(resourceName);
      assertThat(c.getModuleId()).isEqualTo(moduleId);
      assertThat(c.getStatus()).isEqualTo(status);
      if (withDetails) {
        assertThat(c.getDetails()).isNotNull();
      }
    });
  }

  @TestConfiguration
  static class ConfirmationTopicConfiguration {

    @Bean
    public NewTopic confirmationTopic() {
      return new NewTopic(CONFIRMATION_TOPIC, 1, (short) 1);
    }
  }
}
