package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.integration.kafka.model.ResourceEventType.DELETE;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.permission.FolioPermissionService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityKafkaEventHandlerTest {

  private static final String MODULE_ID = "test-module-1.0.0";

  @InjectMocks private CapabilityKafkaEventHandler eventHandler;
  @Spy private final ObjectMapper objectMapper = OBJECT_MAPPER;
  @Mock private CapabilityService capabilityService;
  @Mock private FolioPermissionService folioPermissionService;
  @Mock private CapabilityEventProcessor capabilityEventProcessor;
  @Mock private CapabilitySetDescriptorService capabilitySetDescriptorService;

  @Test
  void handleEvent_positive_capabilityCreateEvent() {
    var newValueMap = capabilityEventBodyAsMap();
    when(capabilityEventProcessor.process(capabilityEvent())).thenReturn(capabilityResultHolder());
    when(capabilityEventProcessor.process(null)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(CREATE)
      .newValue(newValueMap)
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(List.of(permission()), emptyList());
    verify(capabilityService).update(CREATE, List.of(capability()), emptyList());
    verify(capabilitySetDescriptorService).update(CREATE, emptyList(), emptyList());
    verify(objectMapper).convertValue(newValueMap, CapabilityEvent.class);
  }

  @Test
  void handleEvent_positive_capabilityDeleteEvent() {
    var oldValueMap = capabilityEventBodyAsMap();
    when(capabilityEventProcessor.process(null)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityEventProcessor.process(capabilityEvent())).thenReturn(capabilityResultHolder());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(DELETE)
      .oldValue(oldValueMap)
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(emptyList(), List.of(permission()));
    verify(capabilityService).update(DELETE, emptyList(), List.of(capability()));
    verify(capabilitySetDescriptorService).update(DELETE, emptyList(), emptyList());
    verify(objectMapper).convertValue(oldValueMap, CapabilityEvent.class);
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

  private static CapabilityResultHolder capabilityResultHolder(List<Capability> capabilities,
    List<CapabilitySetDescriptor> capabilitySetDescriptors) {
    return new CapabilityResultHolder(capabilities, capabilitySetDescriptors);
  }

  private static Capability capability() {
    return new Capability()
      .resource("Test-Resource Item")
      .type(DATA)
      .action(VIEW)
      .permission("test-resource.item.get");
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
        "endpoints", List.of(Map.of("path", "/test-items/{id}", "method", "GET")))));
  }
}
