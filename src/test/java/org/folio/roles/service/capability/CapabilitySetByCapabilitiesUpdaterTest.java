package org.folio.roles.service.capability;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitySetByCapabilitiesUpdaterTest {

  @InjectMocks private CapabilitySetByCapabilitiesUpdater capabilitySetByCapabilitiesUpdater;
  @Mock private CapabilitySetMapper capabilitySetMapper;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityService capabilityService;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Test
  void update_positive() {
    var capabilitySetId = randomUUID();
    var capabilitySet = new CapabilitySet().name("capabilitySet").id(capabilitySetId);
    var capability = new Capability().id(randomUUID()).name("existCapability");
    var existCapabilities = List.of(capability);
    var newCapabilityId = randomUUID();
    var newCapability = new Capability().id(newCapabilityId).name("newCapability");
    var extendedCapabilitySet = new ExtendedCapabilitySet();
    var updatedCapabilities = List.of(capability, newCapability);

    when(capabilityService.findByCapabilitySetIdsIncludeDummy(Set.of(capabilitySetId)))
      .thenReturn(existCapabilities);
    when(capabilitySetMapper.toExtendedCapabilitySet(capabilitySet, existCapabilities))
      .thenReturn(extendedCapabilitySet);
    when(capabilitySetMapper.toExtendedCapabilitySet(capabilitySet, updatedCapabilities))
      .thenReturn(extendedCapabilitySet);

    capabilitySetByCapabilitiesUpdater.update(capabilitySet, List.of(newCapability));

    verify(capabilitySetService).addCapabilitiesById(capabilitySet.getId(), List.of(newCapabilityId));
    verify(applicationEventPublisher).publishEvent(isA(CapabilitySetEvent.class));
  }
}
