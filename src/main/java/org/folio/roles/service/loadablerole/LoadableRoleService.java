package org.folio.roles.service.loadablerole;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
  public LoadableRole getById(UUID id) {
    var entity = repository.getReferenceById(id);
    return mapper.toRole(entity);
  }

  @Transactional(readOnly = true)
  public Optional<LoadableRole> findByIdOrName(UUID id, String name) {
    return repository.findByIdOrName(id, name)
      .map(mapper::toRole);
  }

  @Transactional(readOnly = true)
  public List<LoadableRole> findAllByPermissions(Set<String> permissionNames) {
    var entities = repository.findAllByPermissions(permissionNames);
    return mapper.toRoles(entities);
  }

  public LoadableRole save(LoadableRole role) {
    requireNonNull(role.getId(), "Loadable role id is null");

    var entity = mapper.toRoleEntity(role);
    var saved = repository.save(entity);

    return mapper.toRole(saved);
  }

  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
