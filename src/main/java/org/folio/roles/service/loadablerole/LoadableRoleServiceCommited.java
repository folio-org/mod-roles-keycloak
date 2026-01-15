package org.folio.roles.service.loadablerole;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.repository.LoadableRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class LoadableRoleServiceCommited {

  private final LoadableRoleRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void save(LoadableRoleEntity entity) {
    repository.save(entity);
  }
}
