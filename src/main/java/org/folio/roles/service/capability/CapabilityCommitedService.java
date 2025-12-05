package org.folio.roles.service.capability;

import java.util.List;
import lombok.AllArgsConstructor;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.repository.CapabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CapabilityCommitedService {

  private final CapabilityRepository capabilityRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<CapabilityEntity> saveAll(List<CapabilityEntity> capabilityEntities) {
    return capabilityRepository.saveAll(capabilityEntities);
  }
}
