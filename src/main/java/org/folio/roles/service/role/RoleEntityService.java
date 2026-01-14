package org.folio.roles.service.role;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.domain.model.event.TenantPermissionsChangedEvent;
import org.folio.roles.mapper.entity.RoleEntityMapper;
import org.folio.roles.repository.RoleEntityRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class RoleEntityService {

  private final RoleEntityRepository repository;
  private final RoleEntityMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  public Role create(Role role) {
    var roleEntity = mapper.toRoleEntity(role);
    var savedRoleEntity = repository.save(roleEntity);
    log.debug("Role has been saved: id = {}, name = {}", role.getId(), role.getName());
    return mapper.toRole(savedRoleEntity);
  }

  @Transactional(readOnly = true)
  public boolean existById(UUID id) {
    return repository.existsById(id);
  }

  public Role update(Role role) {
    requireNonNull(role.getId(), "To update role, role ID cannot be null.");
    checkIfRoleExists(role.getId());
    var roleEntity = mapper.toRoleEntity(role);
    var savedRoleEntity = repository.save(roleEntity);
    log.debug("Role has been updated: id = {}, name = {}", role.getId(), role.getName());
    return mapper.toRole(savedRoleEntity);
  }

  public void deleteById(UUID id) {
    repository.deleteById(id);
    eventPublisher.publishEvent(TenantPermissionsChangedEvent.tenantPermissionsChanged());
  }

  @Transactional(readOnly = true)
  public Role getById(UUID id) {
    var roleEntity = repository.getReferenceById(id);
    log.debug("Role has been found: id = {}, name = {}", roleEntity.getId(), roleEntity.getName());
    return mapper.toRole(roleEntity);
  }

  @Transactional(readOnly = true)
  public List<Role> findByIds(List<UUID> ids) {
    var roleEntity = repository.findByIdIn(ids);
    return mapper.toRole(roleEntity);
  }

  @Transactional(readOnly = true)
  public PageResult<Role> findByQuery(String query, Integer offset, Integer limit) {
    var offsetRequest = OffsetRequest.of(offset, limit);
    var roleEntities = isNotBlank(query)
      ? repository.findByCql(query, offsetRequest)
      : repository.findAll(offsetRequest);
    log.debug("Roles have been found: count = {}", roleEntities.getTotalElements());
    var rolesPages = roleEntities.map(mapper::toRole);
    return PageResult.fromPage(rolesPages);
  }

  public Optional<Role> findByName(String roleName) {
    return repository.findByName(roleName).map(mapper::toRole);
  }

  private void checkIfRoleExists(UUID id) {
    repository.getReferenceById(id);
  }
}
