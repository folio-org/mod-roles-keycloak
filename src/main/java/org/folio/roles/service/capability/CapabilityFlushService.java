package org.folio.roles.service.capability;

import java.util.List;
import lombok.AllArgsConstructor;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.repository.CapabilityRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CapabilityFlushService {

  private final CapabilityRepository capabilityRepository;

  public List<CapabilityEntity> saveAll(List<CapabilityEntity> capabilityEntities) {
    return capabilityRepository.saveAllAndFlush(capabilityEntities);
  }
}
