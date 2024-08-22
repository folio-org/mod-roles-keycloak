package org.folio.roles.service.role;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.dto.UserRoles;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.domain.entity.key.UserRoleKey;
import org.folio.roles.mapper.entity.UserRoleMapper;
import org.folio.roles.repository.UserRoleRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class UserRoleEntityService {

  private final UserRoleMapper mapper;
  private final UserRoleRepository repository;

  /**
   * Creates a new {@link UserRole} record in the database.
   *
   * @param userId - {@link List} of {@link UserRole} object to save.
   * @return saved {@link UserRole} object.
   */
  public List<UserRole> create(UUID userId, List<UUID> roleIds) {
    var foundRelations = findExistingUserRoles(userId, roleIds);
    if (isNotEmpty(foundRelations)) {
      throw new EntityExistsException(String.format(
        "Relations between user and roles already exists (userId: %s, roles: %s)", userId, roleIds));
    }

    var userRoles = mapItems(roleIds, roleId -> new UserRole().userId(userId).roleId(roleId));
    var userRoleEntities = mapper.toEntity(userRoles);
    var savedEntities = repository.saveAll(userRoleEntities);
    return mapper.toDto(savedEntities);
  }

  /**
   * Creates a new {@link UserRole} record in the database.
   *
   * @param userRole - {@link UserRole} to be created
   * @return saved {@link UserRole} object.
   */
  public UserRole createSafe(UserRole userRole) {
    var userRoleEntity = mapper.toEntity(userRole);
    var savedEntity = repository.save(userRoleEntity);
    return mapper.toDto(savedEntity);
  }

  public void delete(UUID userId, List<UUID> roleIds) {
    repository.deleteByUserIdAndRoleIdIn(userId, roleIds);
    log.debug("User roles have been deleted: userId = {}, roleIds = {}", userId, roleIds);
  }

  public void deleteByUserId(UUID userId) {
    var userRoles = repository.findByUserId(userId);
    if (CollectionUtils.isEmpty(userRoles)) {
      throw new EntityNotFoundException("There are no assigned roles for userId: " + userId);
    }
    repository.deleteByUserId(userId);
    log.debug("User roles have been deleted: userId = {}", userId);
  }

  @Transactional(readOnly = true)
  public List<UserRole> findByUserId(UUID userId) {
    var rolesUserEntity = repository.findByUserId(userId);
    return mapper.toDto(rolesUserEntity);
  }

  @Transactional(readOnly = true)
  public Optional<UserRole> find(UserRole userRole) {
    var rolesUserEntity = repository.findById(UserRoleKey.of(userRole.getUserId(), userRole.getRoleId()));
    return rolesUserEntity.map(mapper::toDto);
  }

  @Transactional(readOnly = true)
  public UserRoles findByQuery(String query, Integer offset, Integer limit) {
    var offsetRequest = OffsetRequest.of(offset, limit);
    var pagedResult = isNotBlank(query)
      ? repository.findByCql(query, offsetRequest)
      : repository.findAll(offsetRequest);

    return new UserRoles()
      .userRoles(mapper.toDto(pagedResult.getContent()))
      .totalRecords(pagedResult.getTotalPages());
  }

  private List<UUID> findExistingUserRoles(UUID userId, List<UUID> roleIds) {
    return emptyIfNull(repository.findByUserIdAndRoleIdIn(userId, roleIds)).stream()
      .map(UserRoleEntity::getRoleId)
      .collect(Collectors.toList());
  }
}
