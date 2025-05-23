package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.integration.kafka.model.ResourceEventType.DELETE;
import static org.folio.roles.integration.kafka.model.ResourceEventType.UPDATE;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityReplacementsService;
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
  private static final String MODULE_ID_V2 = "test-module-1.1.0";

  @InjectMocks private CapabilityKafkaEventHandler eventHandler;
  @Spy private final ObjectMapper objectMapper = OBJECT_MAPPER;
  @Mock private CapabilityService capabilityService;
  @Mock private FolioPermissionService folioPermissionService;
  @Mock private CapabilityEventProcessor capabilityEventProcessor;
  @Mock private CapabilitySetDescriptorService capabilitySetDescriptorService;
  @Mock private CapabilityReplacementsService capabilityReplacementsService;
  @Mock private CapabilitySetByDummyUpdater capabilitySetByDummyUpdater;

  @Test
  void handleEvent_positive_capabilityCreateEvent() {
    var newValueMap = capabilityEventBodyAsMap();
    when(capabilityEventProcessor.process(capabilityEvent())).thenReturn(capabilityResultHolder());
    when(capabilityEventProcessor.process(null)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

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
    verify(capabilityService).updateAppAndModuleVersionByAppAndModuleName("test-application", "test-module",
      "test-application-0.0.1", "test-module-1.0.0");
    verify(capabilitySetDescriptorService).updateAppAndModuleVersionByAppAndModuleName("test-application",
      "test-module", "test-application-0.0.1", "test-module-1.0.0");
  }

  @Test
  void handleEvent_positive_capabilityDeleteEvent() {
    var oldValueMap = capabilityEventBodyAsMap();
    when(capabilityEventProcessor.process(null)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityEventProcessor.process(capabilityEvent())).thenReturn(capabilityResultHolder());
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

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

  @Test
  void handleEvent_positive_capabilityUpdatedEvent() {
    var oldEvent = capabilityEvent(MODULE_ID, List.of(folioResource()));
    var newEvent = capabilityEvent(MODULE_ID_V2, List.of(folioResource()));
    when(capabilityEventProcessor.process(oldEvent)).thenReturn(capabilityResultHolder());
    when(capabilityEventProcessor.process(newEvent)).thenReturn(capabilityResultHolder());
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(capabilityEventBodyAsMap(MODULE_ID, List.of(sampleResources())))
      .newValue(capabilityEventBodyAsMap(MODULE_ID_V2, List.of(sampleResources())))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(List.of(permission()), List.of(permission()));
    verify(capabilityService).update(UPDATE, List.of(capability()), List.of(capability()));
    verify(capabilitySetDescriptorService).update(UPDATE, emptyList(), emptyList());
    verify(objectMapper, times(2)).convertValue(anyMap(), eq(CapabilityEvent.class));
    verify(capabilityService).updateAppAndModuleVersionByAppAndModuleName("test-application", "test-module",
      "test-application-0.0.1", "test-module-1.1.0");
    verify(capabilitySetDescriptorService).updateAppAndModuleVersionByAppAndModuleName("test-application",
      "test-module", "test-application-0.0.1", "test-module-1.1.0");
  }

  @Test
  void handleEvent_positive_resourcesAddedWithSameModuleId() {
    var oldEvent = capabilityEvent(MODULE_ID, null);
    var newEvent = capabilityEvent(MODULE_ID, List.of(folioResource()));
    when(capabilityEventProcessor.process(oldEvent)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityEventProcessor.process(newEvent)).thenReturn(capabilityResultHolder());
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(capabilityEventBodyAsMap(MODULE_ID, emptyList()))
      .newValue(capabilityEventBodyAsMap(MODULE_ID, List.of(sampleResources())))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(List.of(permission()), emptyList());
    verify(capabilityService).update(UPDATE, List.of(capability()), emptyList());
    verify(capabilitySetDescriptorService).update(UPDATE, emptyList(), emptyList());
    verify(objectMapper, times(2)).convertValue(anyMap(), eq(CapabilityEvent.class));
  }

  @Test
  void handleEvent_positive_resourcesDeletedWithSameModuleId() {
    var oldEvent = capabilityEvent(MODULE_ID, List.of(folioResource()));
    var newEvent = capabilityEvent(MODULE_ID, null);
    when(capabilityEventProcessor.process(oldEvent)).thenReturn(capabilityResultHolder());
    when(capabilityEventProcessor.process(newEvent)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(capabilityEventBodyAsMap(MODULE_ID, List.of(sampleResources())))
      .newValue(capabilityEventBodyAsMap(MODULE_ID, emptyList()))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(emptyList(), List.of(permission()));
    verify(capabilityService).update(UPDATE, emptyList(), List.of(capability()));
    verify(capabilitySetDescriptorService).update(UPDATE, emptyList(), emptyList());
    verify(objectMapper, times(2)).convertValue(anyMap(), eq(CapabilityEvent.class));
  }

  @Test
  void handleEvent_positive_capabilityUpdatedEventResourcesCreated() {
    var oldEvent = capabilityEvent(MODULE_ID, null);
    var newEvent = capabilityEvent(MODULE_ID_V2, List.of(folioResource()));
    when(capabilityEventProcessor.process(oldEvent)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityEventProcessor.process(newEvent)).thenReturn(capabilityResultHolder());
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(capabilityEventBodyAsMap(MODULE_ID, emptyList()))
      .newValue(capabilityEventBodyAsMap(MODULE_ID_V2, List.of(sampleResources())))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(List.of(permission()), emptyList());
    verify(capabilityService).update(UPDATE, List.of(capability()), emptyList());
    verify(capabilitySetDescriptorService).update(UPDATE, emptyList(), emptyList());
    verify(objectMapper, times(2)).convertValue(anyMap(), eq(CapabilityEvent.class));
  }

  @Test
  void handleEvent_positive_capabilityUpdatedEventResourcesDeleted() {
    var newEvent = capabilityEvent(MODULE_ID_V2, null);
    var oldEvent = capabilityEvent(MODULE_ID, List.of(folioResource()));
    when(capabilityEventProcessor.process(newEvent)).thenReturn(capabilityResultHolder(emptyList(), emptyList()));
    when(capabilityEventProcessor.process(oldEvent)).thenReturn(capabilityResultHolder());
    when(capabilityReplacementsService.deduceReplacements(any())).thenReturn(Optional.empty());

    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(capabilityEventBodyAsMap(MODULE_ID, List.of(sampleResources())))
      .newValue(capabilityEventBodyAsMap(MODULE_ID_V2, emptyList()))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(folioPermissionService).update(emptyList(), List.of(permission()));
    verify(capabilityService).update(UPDATE, emptyList(), List.of(capability()));
    verify(capabilitySetDescriptorService).update(UPDATE, emptyList(), emptyList());
    verify(objectMapper, times(2)).convertValue(anyMap(), eq(CapabilityEvent.class));
  }

  @Test
  void handleEvent_positive_applicationVersionUpgradeEvent() {
    var resourceEvent = ResourceEvent.builder()
      .tenant(TENANT_ID)
      .type(UPDATE)
      .oldValue(Map.of("moduleId", MODULE_ID, "moduleType", "module", "applicationId", APPLICATION_ID))
      .newValue(Map.of("moduleId", MODULE_ID, "moduleType", "module", "applicationId", APPLICATION_ID_V2))
      .build();

    eventHandler.handleEvent(resourceEvent);

    verify(capabilityService).updateApplicationVersion(MODULE_ID, APPLICATION_ID_V2, APPLICATION_ID);
    verify(capabilitySetDescriptorService).updateApplicationVersion(MODULE_ID, APPLICATION_ID_V2, APPLICATION_ID);
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
    return capabilityEvent(MODULE_ID, List.of(folioResource()));
  }

  private static CapabilityEvent capabilityEvent(String moduleId, List<FolioResource> folioResources) {
    return new CapabilityEvent()
      .moduleType(MODULE)
      .moduleId(moduleId)
      .applicationId(APPLICATION_ID)
      .resources(folioResources);
  }

  private static FolioResource folioResource() {
    return new FolioResource().endpoints(List.of(endpoint())).permission(permission());
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
    return capabilityEventBodyAsMap(MODULE_ID, List.of(sampleResources()));
  }

  private static Map<String, Object> capabilityEventBodyAsMap(String moduleId, List<Map<String, Object>> resources) {
    return Map.of(
      "moduleId", moduleId,
      "moduleType", "module",
      "applicationId", APPLICATION_ID,
      "resources", resources);
  }

  private static Map<String, Object> sampleResources() {
    return Map.of(
      "permission", Map.of(
        "permissionName", "test-resource.item.get",
        "displayName", "Test-Resource Item - Get by ID",
        "description", "Get test-resource item by id"),
      "endpoints", List.of(Map.of("path", "/test-items/{id}", "method", "GET")));
  }
}
