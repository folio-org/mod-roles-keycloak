package org.folio.roles.service.migration;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.service.role.UserRoleService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class MigrationRoleCreator {

  private final RoleService roleService;
  private final org.folio.roles.service.role.RoleMigrationService roleMigrationService;
  private final UserRoleService userRoleService;
  private final MigrationErrorService migrationErrorService;

  /**
   * Creates a list of role from user permissions.
   *
   * <p>This method handles partial failures gracefully:
   * <ul>
   *   <li>If a role already exists in DB or Keycloak, it will be retrieved</li>
   *   <li>If role creation fails, it will be skipped and logged to database</li>
   *   <li>No partial data will be left (rollback is handled by RoleService)</li>
   * </ul>
   *
   * @param userPermissions - user permissions list
   * @param jobId - migration job identifier for error logging
   * @return {@link List} with created {@link Role} objects (only successfully created or existing roles)
   */
  @Transactional
  public List<Role> createRoles(List<UserPermissions> userPermissions, UUID jobId) {
    var roleNames = extractUniqueRoleNames(userPermissions);
    log.info("Creating {} role(s)...", roleNames.size());
    
    var roles = mapItems(roleNames, MigrationRoleCreator::createRole);
    var creationResult = roleMigrationService.createRolesSafely(roles);
    
    logCreationErrors(creationResult, jobId);
    
    var createdRoles = creationResult.getSuccessfulRoles();
    log.info("Roles created successfully: {} out of {} requested", createdRoles.size(), roles.size());

    return handlePartialFailures(roleNames, createdRoles, jobId);
  }

  private List<String> extractUniqueRoleNames(List<UserPermissions> userPermissions) {
    return toStream(userPermissions)
      .map(UserPermissions::getRoleName)
      .distinct()
      .toList();
  }

  private void logCreationErrors(org.folio.roles.service.role.RoleCreationResult result, UUID jobId) {
    if (!result.hasFailures()) {
      return;
    }

    log.warn("Recording {} role creation failure(s)", result.getFailures().size());
    
    for (var failure : result.getFailures()) {
      logRoleCreationError(jobId, failure.getRoleName(), failure.getFullErrorMessage());
    }
  }

  private List<Role> handlePartialFailures(List<String> expectedRoleNames, List<Role> createdRoles, UUID jobId) {
    if (createdRoles.size() >= expectedRoleNames.size()) {
      return createdRoles;
    }

    log.warn("Some roles failed to create or already existed. Expected: {}, Successfully created/found: {}",
      expectedRoleNames.size(), createdRoles.size());

    var missingRoleNames = findMissingRoleNames(expectedRoleNames, createdRoles);
    if (isEmpty(missingRoleNames)) {
      return createdRoles;
    }

    return searchAndCombineRoles(createdRoles, missingRoleNames, jobId);
  }

  private List<String> findMissingRoleNames(List<String> expectedNames, List<Role> createdRoles) {
    var createdRoleNames = toStream(createdRoles).map(Role::getName).toList();
    return expectedNames.stream()
      .filter(name -> createdRoleNames.stream().noneMatch(name::equals))
      .toList();
  }

  private List<Role> searchAndCombineRoles(List<Role> createdRoles, List<String> missingRoleNames, UUID jobId) {
    log.info("Searching for {} missing role(s)...", missingRoleNames.size());
    var foundRoles = searchForMissingRoles(missingRoleNames, jobId);
    
    if (isEmpty(foundRoles)) {
      return createdRoles;
    }

    log.info("Found {} existing role(s)", foundRoles.size());
    return combineRoles(createdRoles, foundRoles);
  }

  private List<Role> searchForMissingRoles(List<String> roleNames, UUID jobId) {
    var foundRoles = new ArrayList<Role>();
    
    for (String roleName : roleNames) {
      searchSingleRole(roleName, jobId).ifPresent(foundRoles::add);
    }
    
    return foundRoles;
  }

  private Optional<Role> searchSingleRole(String roleName, UUID jobId) {
    try {
      var rolesFound = roleService.search("name==" + roleName, 0, 1);
      if (rolesFound.getRoles() != null && !rolesFound.getRoles().isEmpty()) {
        return Optional.of(rolesFound.getRoles().get(0));
      }
    } catch (Exception e) {
      log.warn("Failed to search for role: name = {}", roleName, e);
    }
    return Optional.empty();
  }

  private List<Role> combineRoles(List<Role> createdRoles, List<Role> foundRoles) {
    var allRoles = new ArrayList<>(createdRoles);
    allRoles.addAll(foundRoles);
    return allRoles;
  }

  private void logRoleCreationError(UUID jobId, String roleName, String errorMessage) {
    migrationErrorService.logError(
      jobId,
      "ROLE_CREATION_FAILED",
      errorMessage,
      "ROLE",
      roleName
    );
  }
  

  /**
   * Assigns users to a role from the given list of {@link UserPermissions} objects.
   *
   * @param userPermissions - user permissions list
   */
  @Transactional
  public void assignUsers(List<UserPermissions> userPermissions) {
    var userByRole = groupUsersByRole(userPermissions);
    var counter = new AtomicInteger(0);
    var errorPairs = new ArrayList<UserRole>();
    for (var userByRoleEntry : userByRole.entrySet()) {
      var roleId = userByRoleEntry.getKey();
      for (var userId : userByRoleEntry.getValue()) {
        var userRole = new UserRole().userId(userId).roleId(roleId);
        try {
          userRoleService.createSafe(userRole);
          counter.getAndIncrement();
        } catch (Exception exception) {
          log.warn("Failed to assign user {} to role {}", userId, roleId, exception);
          errorPairs.add(userRole);
        }
      }
    }

    if (isNotEmpty(errorPairs)) {
      throw new MigrationException("Failed to assign users to roles: " + errorPairs);
    }

    log.info("User-role relations creation process finished: totalRecords = {}", counter.get());
  }

  private static Map<UUID, List<UUID>> groupUsersByRole(List<UserPermissions> userPermissions) {
    return toStream(userPermissions)
      .collect(groupingBy(up -> up.getRole().getId(), mapping(UserPermissions::getUserId, toList())));
  }

  private static Role createRole(String roleName) {
    return new Role()
      .name(roleName)
      .type(RoleType.REGULAR)
      .description("System generated role during migration");
  }
}
