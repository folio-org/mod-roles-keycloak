package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.RESOURCE_NAME;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitySetDescriptorServiceTest {

  @InjectMocks private CapabilitySetDescriptorService capabilitySetDescriptorService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetMapper capabilitySetMapper;
  @Mock private CapabilitySetService capabilitySetService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void createSafe_positive() {
    var capability = capability(CAPABILITY_ID, "Foo", CREATE, "foo.item.create");
    var capabilitySetDescriptor = capabilitySetDescriptor(Map.of("Foo", List.of(CREATE)));

    when(capabilityService.findByNames(List.of("foo.create"))).thenReturn(List.of(capability));
    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName("test_resource.create")).thenReturn(false);

    capabilitySetDescriptorService.createSafe(APPLICATION_ID, List.of(capabilitySetDescriptor));

    verify(capabilitySetService).create(capabilitySet(List.of(CAPABILITY_ID)));
  }

  @Test
  void createSafe_positive_emptyCapabilitySetDescriptors() {
    capabilitySetDescriptorService.createSafe(APPLICATION_ID, emptyList());
    verifyNoInteractions(capabilityService, capabilitySetService, capabilitySetMapper);
  }

  @Test
  void createSafe_positive_duplicatedCapabilitySet() {
    var capability = capability(CAPABILITY_ID, "Foo", CREATE, "foo.item.create");
    var capabilitySetDescriptor = capabilitySetDescriptor(Map.of("Foo", List.of(CREATE)));

    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName("test_resource.create")).thenReturn(false);
    when(capabilityService.findByNames(List.of("foo.create"))).thenReturn(List.of(capability));

    var capabilitySetDescriptors = List.of(capabilitySetDescriptor, capabilitySetDescriptor);
    capabilitySetDescriptorService.createSafe(APPLICATION_ID, capabilitySetDescriptors);

    verify(capabilitySetService).create(capabilitySet(List.of(CAPABILITY_ID)));
  }

  @Test
  void createSafe_positive_capabilitySetAlreadyExists() {
    var capabilitySetName = "test_resource.create";
    var capabilitySetDescriptor = capabilitySetDescriptor(Map.of("Foo", List.of(CREATE)));

    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName(capabilitySetName)).thenReturn(true);

    capabilitySetDescriptorService.createSafe(APPLICATION_ID, List.of(capabilitySetDescriptor));

    verify(capabilitySetService, never()).create(any(CapabilitySet.class));
  }

  @Test
  void createSafe_positive_capabilitySetWithoutCapabilities() {
    var capabilitySetDescriptor = capabilitySetDescriptor(emptyMap());
    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName("test_resource.create")).thenReturn(false);

    capabilitySetDescriptorService.createSafe(APPLICATION_ID, List.of(capabilitySetDescriptor));

    verify(capabilitySetService, never()).create(any(CapabilitySet.class));
  }

  @Test
  void createSafe_positive_capabilitySetWithoutActions() {
    var capabilitySetDescriptor = capabilitySetDescriptor(Map.of("Foo", emptyList()));
    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName("test_resource.create")).thenReturn(false);

    capabilitySetDescriptorService.createSafe(APPLICATION_ID, List.of(capabilitySetDescriptor));

    verify(capabilitySetService, never()).create(any(CapabilitySet.class));
  }

  @Test
  void createSafe_positive_capabilitySetWithNotFoundCapability() {
    var capabilitySetDescriptor = capabilitySetDescriptor(Map.of("Foo", List.of(CREATE)));
    when(capabilitySetMapper.convert(APPLICATION_ID, capabilitySetDescriptor)).thenReturn(capabilitySet());
    when(capabilitySetService.existsByName("test_resource.create")).thenReturn(false);
    when(capabilityService.findByNames(List.of("foo.create"))).thenReturn(emptyList());

    capabilitySetDescriptorService.createSafe(APPLICATION_ID, List.of(capabilitySetDescriptor));

    verify(capabilitySetService, never()).create(any(CapabilitySet.class));
  }

  private static CapabilitySetDescriptor capabilitySetDescriptor(Map<String, List<CapabilityAction>> capabilities) {
    var capabilitySetDescriptor = new CapabilitySetDescriptor();
    capabilitySetDescriptor.setResource(RESOURCE_NAME);
    capabilitySetDescriptor.setAction(CREATE);
    capabilitySetDescriptor.setType(DATA);
    capabilitySetDescriptor.setDescription("test capability description");
    capabilitySetDescriptor.setCapabilities(capabilities);
    return capabilitySetDescriptor;
  }
}
