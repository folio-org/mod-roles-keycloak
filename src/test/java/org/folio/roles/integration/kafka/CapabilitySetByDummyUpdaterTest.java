package org.folio.roles.integration.kafka;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetByCapabilitiesUpdater;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitySetByDummyUpdaterTest {

  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetEntityMapper capabilitySetEntityMapper;
  @Mock private CapabilitySetByCapabilitiesUpdater capabilitySetByCapabilitiesUpdater;
  @InjectMocks private CapabilitySetByDummyUpdater capabilitySetByDummyUpdater;

  @Test
  void update_positive() {
    var capabilitySet = new CapabilitySet().name("dummy").id(UUID.randomUUID());
    var capabilitySetEntity = new CapabilitySetEntity();
    capabilitySetEntity.setId(capabilitySet.getId());
    capabilitySetEntity.setName(capabilitySet.getName());
    var capabilitySetOpt = Optional.of(capabilitySet);
    var capabilityToAdd = new Capability().name("capabilityToAdd").id(UUID.randomUUID());
    var relatedCapabilitySetEntity = new CapabilitySetEntity();
    relatedCapabilitySetEntity.setName("relatedCapabilitySet");
    relatedCapabilitySetEntity.setId(UUID.randomUUID());
    var relatedCapabilitySet = new CapabilitySet()
      .name(relatedCapabilitySetEntity.getName())
      .id(relatedCapabilitySetEntity.getId());
    var capabilitiesToAdd = List.of(capabilityToAdd);

    when(capabilitySetService.findByName("dummy")).thenReturn(capabilitySetOpt);
    when(capabilityService.findByCapabilitySetIdsIncludeDummy(Set.of(capabilitySet.getId())))
      .thenReturn(capabilitiesToAdd);
    when(capabilitySetService.findByCapabilityName("dummy"))
      .thenReturn(List.of(relatedCapabilitySetEntity, capabilitySetEntity));
    when(capabilitySetEntityMapper.convert(relatedCapabilitySetEntity)).thenReturn(relatedCapabilitySet);
    when(capabilitySetEntityMapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

    capabilitySetByDummyUpdater.update(List.of("dummy"));

    verify(capabilitySetService).findByCapabilityName("dummy");
    verify(capabilitySetService).findByCapabilityName("relatedCapabilitySet");
    verify(capabilitySetByCapabilitiesUpdater)
      .updateCapabilitySetByCapabilities(List.of(capabilityToAdd), relatedCapabilitySet);
  }
}
