package org.folio.roles.integration.kafka;

import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Map;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  private static final String MODULE_ID = "test-module-1.0.0";

  @InjectMocks private KafkaMessageListener kafkaMessageListener;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private CapabilityKafkaEventHandler capabilityKafkaEventHandler;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(folioModuleMetadata, capabilityKafkaEventHandler);
  }

  @Test
  void handleCapabilityEvent_positive() {
    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .newValue(capabilityEventBodyAsMap())
      .build();

    kafkaMessageListener.handleCapabilityEvent(resourceEvent);

    verify(capabilityKafkaEventHandler).handleEvent(resourceEvent);
  }

  private static Map<String, Object> capabilityEventBodyAsMap() {
    return Map.of(
      "moduleId", MODULE_ID,
      "moduleType", "module",
      "applicationId", APPLICATION_ID,
      "resources", List.of(Map.of(
        "permission", Map.of(
          "permissionName", "test-resource.item.get",
          "displayName", "Test-Resource Item - Get by ID",
          "description", "Get test-resource item by id"),
        "endpoints", List.of(Map.of("path", "/test-items/{id}", "method", "GET")))
      ));
  }
}
