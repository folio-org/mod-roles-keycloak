package org.folio.roles.service.role;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.mapper.entity.LoadableRoleEntityMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class LoadableRoleService {

  private final LoadableRoleRepository repository;
  private final LoadableRoleEntityMapper mapper;

  @Transactional(readOnly = true)
  public Role getById(UUID id) {
    var entity = repository.getReferenceById(id);
    log.debug("Loadable role has been found: id = {}, name = {}", entity.getId(), entity.getName());
    return mapper.toRole(entity);
  }

  @Transactional(readOnly = true)
  public boolean existsById(UUID id) {
    return repository.existsById(id);
  }

  public LoadableRole save(LoadableRole role) {
    var entity = mapper.toRoleEntity(role);
    var saved = repository.save(entity);
    log.debug("Loadable role has been saved: id = {}, name = {}", role.getId(), role.getName());
    return mapper.toRole(saved);
  }

  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
