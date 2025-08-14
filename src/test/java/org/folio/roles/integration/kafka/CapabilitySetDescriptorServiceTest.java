package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.extendedCapabilitySet;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.RESOURCE_NAME;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.domain.model.event.DomainEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.repository.PermissionRepository;
import org.folio.roles.service.capability.CapabilityResolver;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;

@UnitTest
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CapabilitySetDescriptorServiceTest {

  private static final String CAPABILITY_SET_NAME = "test_resource.create";

  private CapabilitySetDescriptorService capabilitySetDescriptorService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetMapper mapper;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private PermissionRepository permissionRepository;
  @Mock private CapabilityResolver capabilityResolver;
  @Mock private CapabilityEntityMapper capabilityMapper;
  @Captor private ArgumentCaptor<DomainEvent> eventCaptor;

  @BeforeEach
  void setUp() {
    var folioExecutionContext = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    this.capabilitySetDescriptorService = new CapabilitySetDescriptorService(
      capabilityService, mapper, capabilitySetService, folioExecutionContext, applicationEventPublisher,
      permissionRepository, capabilityResolver, capabilityMapper);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void update_positive_createdCapabilitySetDescriptor() {
    var capability = capability(CAPABILITY_ID, "Foo", CREATE, "foo.item.create");
    var capabilities = List.of(capability);
    var capabilitySetDescriptor = capabilitySetDescriptor(capabilities);
    var capabilitySetsToSave = List.of(capabilitySet(null, List.of(CAPABILITY_ID)));
    var savedSet = capabilitySet(CAPABILITY_SET_ID, List.of(CAPABILITY_ID));
    var extendedCapabilitySet = extendedCapabilitySet(savedSet, capabilities);

    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(emptyList());
    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilityService.findByNamesIncludeDummy(List.of("foo.create"))).thenReturn(capabilities);
    when(capabilitySetService.createAll(capabilitySetsToSave)).thenReturn(List.of(savedSet));
    when(mapper.toExtendedCapabilitySet(savedSet, capabilities)).thenReturn(extendedCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.CREATE, List.of(capabilitySetDescriptor), emptyList());

    verifyCapturedEvents(CapabilitySetEvent.created(extendedCapabilitySet));
  }

  @Test
  void update_positive_emptyCapabilitySetDescriptors() {
    capabilitySetDescriptorService.update(ResourceEventType.CREATE, emptyList(), emptyList());
    verifyNoInteractions(capabilityService, capabilitySetService, mapper);
  }

  @Test
  void update_positive_capabilitySetAlreadyExists(CapturedOutput output) {
    var newCapabilityId = UUID.randomUUID();
    var capability = capability(newCapabilityId, "Foo", CREATE, "foo.item.create");
    var capabilities = List.of(capability());
    var newCapabilities = List.of(capability);
    var capabilitySetDescriptor = capabilitySetDescriptor(List.of(capability));
    var updatedCapabilitySet = capabilitySet(List.of(newCapabilityId));
    var existingCapabilitySet = capabilitySet();
    var capabilitySets = List.of(updatedCapabilitySet);
    var extendedCapabilitySet = extendedCapabilitySet(existingCapabilitySet, capabilities);
    var updatedExtCapabilitySet = extendedCapabilitySet(existingCapabilitySet, capabilities);

    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(List.of(existingCapabilitySet));
    when(capabilityService.findByNamesIncludeDummy(List.of("foo.create"))).thenReturn(newCapabilities);
    when(capabilityService.findByIds(List.of(CAPABILITY_ID))).thenReturn(capabilities);
    when(mapper.toExtendedCapabilitySet(existingCapabilitySet, capabilities)).thenReturn(extendedCapabilitySet);
    when(capabilitySetService.createAll(capabilitySets)).thenReturn(capabilitySets);
    when(mapper.toExtendedCapabilitySet(updatedCapabilitySet, newCapabilities)).thenReturn(updatedExtCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.CREATE, List.of(capabilitySetDescriptor), emptyList());

    verifyCapturedEvents(CapabilitySetEvent.updated(updatedExtCapabilitySet, extendedCapabilitySet));
    assertThat(output.getAll()).contains("Duplicated capability sets has been updated: [test_resource.create]");
  }

  @Test
  void update_positive_capabilitySetWithoutCapabilities(CapturedOutput output) {
    var capabilitySetDescriptor = capabilitySetDescriptor(List.of());
    var capabilitySetsToSave = List.of(capabilitySet(null, emptyList()));
    var savedCapabilitySet = capabilitySet(emptyList());
    var extendedCapabilitySet = extendedCapabilitySet(savedCapabilitySet, emptyList());

    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(emptyList());
    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilitySetService.createAll(capabilitySetsToSave)).thenReturn(List.of(savedCapabilitySet));
    when(mapper.toExtendedCapabilitySet(savedCapabilitySet, emptyList())).thenReturn(extendedCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.CREATE, List.of(capabilitySetDescriptor), emptyList());

    verifyCapturedEvents(CapabilitySetEvent.created(extendedCapabilitySet));
    assertThat(output.getAll()).contains("Capabilities are empty for capability set: name = test_resource.create");
  }

  @Test
  void update_positive_capabilitySetWithoutActions(CapturedOutput output) {
    var capabilitySetsToSave = List.of(capabilitySet(null, emptyList()));
    var savedCapabilitySet = capabilitySet(emptyList());
    var capabilitySetDescriptor = capabilitySetDescriptor(emptyList());
    var extendedCapabilitySet = extendedCapabilitySet(savedCapabilitySet, emptyList());

    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(emptyList());
    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilitySetService.createAll(capabilitySetsToSave)).thenReturn(List.of(savedCapabilitySet));
    when(mapper.toExtendedCapabilitySet(savedCapabilitySet, emptyList())).thenReturn(extendedCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.CREATE, List.of(capabilitySetDescriptor), emptyList());

    verifyCapturedEvents(CapabilitySetEvent.created(extendedCapabilitySet));
    assertThat(output.getAll()).contains(
      "Capabilities are empty for capability set: name = test_resource.create");
  }

  @Test
  void update_positive_capabilitySetWithNotFoundCapability(CapturedOutput output) {
    var dummyCapability = new Capability();
    dummyCapability.setId(UUID.randomUUID());
    dummyCapability.setName("foo.create");
    dummyCapability.setDummyCapability(true);
    var capabilitySetsToSave = List.of(capabilitySet(null, List.of(dummyCapability.getId())));
    var savedCapabilitySet = capabilitySet(emptyList());
    var capabilitySetDescriptor = capabilitySetDescriptor(List.of(dummyCapability));
    var extendedCapabilitySet = extendedCapabilitySet(savedCapabilitySet, emptyList());

    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(emptyList());
    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilityService.findByNamesIncludeDummy(List.of("foo.create"))).thenReturn(emptyList());
    when(capabilitySetService.createAll(capabilitySetsToSave)).thenReturn(List.of(savedCapabilitySet));
    when(mapper.toExtendedCapabilitySet(savedCapabilitySet, emptyList())).thenReturn(extendedCapabilitySet);
    when(capabilityService.save(isA(Capability.class))).thenReturn(dummyCapability);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.CREATE, List.of(capabilitySetDescriptor), emptyList());

    assertThat(output.getAll()).contains("Capability is not found by name: foo.create, creating a dummy one");
    assertThat(output.getAll()).contains("Created dummy capability with name: foo.create");

    var capturedEvents = eventCaptor.getAllValues();
    assertThat(capturedEvents).hasSize(2);

    var capabilityEvent = (CapabilityEvent) capturedEvents.get(0);
    assertThat(capabilityEvent.getNewObject())
      .usingRecursiveComparison()
      .ignoringFields("id", "timestamp", "context")
      .isEqualTo(dummyCapability);

    var capabilitySetEvent = (CapabilitySetEvent) capturedEvents.get(1);
    assertThat(capabilitySetEvent.getNewObject())
      .usingRecursiveComparison()
      .ignoringFields("timestamp", "context")
      .isEqualTo(extendedCapabilitySet);
  }

  @Test
  void update_positive_capabilitySetUpdated(CapturedOutput output) {
    var newCapabilityId = UUID.randomUUID();
    var capability = capability(newCapabilityId, "Foo", CREATE, "foo.item.create");
    var capabilities = List.of(capability());
    var newCapabilities = List.of(capability);
    var capabilitySetDescriptor = capabilitySetDescriptor(newCapabilities);
    var updatedCapabilitySet = capabilitySet(List.of(newCapabilityId));
    var existingCapabilitySet = capabilitySet();
    var capabilitySets = List.of(updatedCapabilitySet);
    var extendedCapabilitySet = extendedCapabilitySet(existingCapabilitySet, capabilities);
    var updatedExtCapabilitySet = extendedCapabilitySet(existingCapabilitySet, capabilities);

    when(mapper.convert(capabilitySetDescriptor)).thenReturn(capabilitySet((UUID) null));
    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(List.of(existingCapabilitySet));
    when(capabilityService.findByNamesIncludeDummy(List.of("foo.create"))).thenReturn(newCapabilities);
    when(capabilityService.findByIds(List.of(CAPABILITY_ID))).thenReturn(capabilities);
    when(mapper.toExtendedCapabilitySet(existingCapabilitySet, capabilities)).thenReturn(extendedCapabilitySet);
    when(capabilitySetService.createAll(capabilitySets)).thenReturn(capabilitySets);
    when(mapper.toExtendedCapabilitySet(updatedCapabilitySet, newCapabilities)).thenReturn(updatedExtCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    capabilitySetDescriptorService.update(ResourceEventType.UPDATE, List.of(capabilitySetDescriptor), emptyList());

    verifyCapturedEvents(CapabilitySetEvent.updated(updatedExtCapabilitySet, extendedCapabilitySet));
    assertThat(output.getAll()).doesNotContain("Duplicated capability sets has been updated");
  }

  @Test
  void update_positive_deprecatedCapabilitySetDescriptor() {
    var existingCapabilitySet = capabilitySet();
    var existingCapabilitySets = List.of(existingCapabilitySet);
    var extendedCapabilitySet = extendedCapabilitySet(existingCapabilitySet, emptyList());

    when(capabilitySetService.findByNames(Set.of(CAPABILITY_SET_NAME))).thenReturn(existingCapabilitySets);
    when(mapper.toExtendedCapabilitySet(existingCapabilitySet, emptyList())).thenReturn(extendedCapabilitySet);
    doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

    var capabilitySetDescriptor = capabilitySetDescriptor(List.of(capability()));
    capabilitySetDescriptorService.update(ResourceEventType.DELETE, emptyList(), List.of(capabilitySetDescriptor));

    verifyCapturedEvents(CapabilitySetEvent.deleted(extendedCapabilitySet));
  }

  @Test
  void updateApplicationVersion_positive() {
    var moduleId = "mod-test-1.0.0";
    capabilitySetDescriptorService.updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
    verify(capabilitySetService).updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
  }

  private static CapabilitySetDescriptor capabilitySetDescriptor(List<Capability> capabilities) {
    var capabilitySetDescriptor = new CapabilitySetDescriptor();
    capabilitySetDescriptor.setResource(RESOURCE_NAME);
    capabilitySetDescriptor.setAction(CREATE);
    capabilitySetDescriptor.setType(DATA);
    capabilitySetDescriptor.setName(getCapabilityName(RESOURCE_NAME, CREATE));
    capabilitySetDescriptor.setDescription("test capability description");
    capabilitySetDescriptor.setCapabilities(capabilities);
    return capabilitySetDescriptor;
  }

  private void verifyCapturedEvents(CapabilitySetEvent... expectedEvents) {
    assertThat(eventCaptor.getAllValues())
      .usingRecursiveComparison()
      .ignoringFields("timestamp", "context")
      .isEqualTo(List.of(expectedEvents));
  }
}
