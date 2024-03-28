package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.permission.FolioPermissionService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  @InjectMocks private KafkaMessageListener kafkaMessageListener;

  @Spy private final ObjectMapper objectMapper = OBJECT_MAPPER;
  @Mock private FolioModuleMetadata metadata;
  @Mock private CapabilityService capabilityService;
  @Mock private FolioPermissionService folioPermissionService;
  @Mock private CapabilityEventProcessor capabilityEventProcessor;
  @Mock private CapabilitySetDescriptorService capabilitySetDescriptorService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(metadata, capabilityService, capabilitySetDescriptorService);
  }

  @Nested
  @DisplayName("handleCapabilityEvent")
  class HandleCapabilityEvent {

    private static final String MODULE_ID = "test-module-1.0.0";

    @Test
    void positive() {
      var newValueMap = Map.of(
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

      when(capabilityEventProcessor.process(capabilityEvent())).thenReturn(capabilityResultHolder());

      var resourceEvent = ResourceEvent.builder()
        .tenant(TENANT_ID)
        .newValue(newValueMap)
        .build();

      kafkaMessageListener.handleCapabilityEvent(resourceEvent);

      verify(folioPermissionService).createIgnoringConflicts(List.of(permission()));
      verify(capabilityService).createSafe(APPLICATION_ID, List.of(capability()));
      verify(capabilitySetDescriptorService).createSafe(APPLICATION_ID, emptyList());
      verify(objectMapper).convertValue(newValueMap, CapabilityEvent.class);
    }

    private static Endpoint endpoint() {
      return new Endpoint().path("/test-items/{id}").method(HttpMethod.GET);
    }

    private static Permission permission() {
      return new Permission()
        .permissionName("test-resource.item.get")
        .displayName("Test-Resource Item - Get by ID")
        .description("Get test-resource item by id");
    }

    private static CapabilityEvent capabilityEvent() {
      return new CapabilityEvent()
        .moduleType(MODULE)
        .moduleId(MODULE_ID)
        .applicationId(APPLICATION_ID)
        .resources(List.of(new FolioResource().endpoints(List.of(endpoint())).permission(permission())));
    }

    private static CapabilityResultHolder capabilityResultHolder() {
      return new CapabilityResultHolder(List.of(capability()), emptyList());
    }

    private static Capability capability() {
      return new Capability()
        .resource("Test-Resource Item")
        .type(DATA)
        .action(VIEW)
        .permission("test-resource.item.get");
    }
  }
}
