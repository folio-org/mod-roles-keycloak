package org.folio.roles.service.loadablerole;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.mapper.entity.LoadableRoleEntityMapper;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class LoadablePermissionService {

  private final LoadablePermissionRepository repository;
  private final LoadableRoleEntityMapper mapper;

  @Transactional(readOnly = true)
  public List<LoadablePermission> findAllByPermissions(Collection<String> permissionNames) {
    var entities = repository.findAllByPermissionNameIn(permissionNames);
    return mapper.toPermission(entities);
  }

  public LoadablePermission save(LoadablePermission perm) {
    var entity = mapper.toPermissionEntity(perm);
    var saved = repository.save(entity);

    return mapper.toPermission(saved);
  }

  public List<LoadablePermission> saveAll(List<LoadablePermission> perms) {
    var entities = mapper.toPermissionEntity(perms);
    var saved = repository.saveAll(entities);

    return mapper.toPermission(saved);
  }
}
