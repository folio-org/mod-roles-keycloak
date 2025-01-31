package org.folio.roles.service.migration;

import static java.util.Collections.emptyList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.permissions.Permissions;
import org.folio.roles.integration.permissions.PermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class UserPermissionsLoader {

  private final Keycloak keycloak;
  private final PermissionsClient permissionsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final KeycloakConfigurationProperties configurationProperties;

  /**
   * Loads user permissions from 'mod-permissions'.
   *
   * @return {@link List} with {@link UserPermissions} relations
   */
  public List<UserPermissions> loadUserPermissions() {
    var tenantId = folioExecutionContext.getTenantId();
    var realm = keycloak.realm(tenantId);
    var usersClient = realm.users();

    var batchSize = configurationProperties.getMigration().getUsersBatchSize();
    var keycloakUsers = loadPaginatedData(batchSize, usersClient::list);
    log.info("Keycloak users are loaded: size = {}", keycloakUsers.size());
    var userIds = getMigratedUserIds(keycloakUsers);
    log.info("Folio user ids found: size = {}", userIds.size());

    var userPermissions = loadUsersPermissions(userIds);
    log.info("User permissions are loaded for users: totalRecords = {}", userPermissions.size());

    return userPermissions;
  }

  private List<UserPermissions> loadUsersPermissions(List<UUID> userIds) {
    return toStream(userIds)
      .map(this::loadUsersPermissions)
      .flatMap(Optional::stream)
      .toList();
  }

  private Optional<UserPermissions> loadUsersPermissions(UUID userId) {
    var permissionNames = permissionsClient.getUserPermissions(userId, "userId", true)
      .map(UserPermissionsLoader::getPermissionNames)
      .orElse(emptyList());

    if (CollectionUtils.isEmpty(permissionNames)) {
      log.debug("User permissions not found: userId = {}", userId);
      return Optional.empty();
    }

    log.debug("User permissions are loaded: userId = {}, totalRecords = {}", userId, permissionNames.size());
    var userPermissions = UserPermissions.of(userId, null, getPermissionsHash(permissionNames),
      permissionNames, new ArrayList<>());
    return Optional.of(userPermissions);
  }

  private static String getPermissionsHash(List<String> permissions) {
    return sha1Hex(String.join("|", permissions)); //NOSONAR not used in secure contexts
  }

  private static List<UUID> getMigratedUserIds(List<UserRepresentation> keycloakUsers) {
    return keycloakUsers.stream()
      .map(UserPermissionsLoader::getUserId)
      .flatMap(Optional::stream)
      .map(UUID::fromString)
      .toList();
  }

  private static Optional<String> getUserId(UserRepresentation keycloakUser) {
    return Optional.ofNullable(keycloakUser.getAttributes())
      .filter(attributes -> attributes.containsKey("user_id"))
      .map(attributes -> attributes.get("user_id"))
      .filter(CollectionUtils::isNotEmpty)
      .map(userIdAttributes -> userIdAttributes.get(0));
  }

  private static List<String> getPermissionNames(Permissions userPermissions) {
    return toStream(userPermissions.getPermissionNames()).distinct().sorted().toList();
  }

  public static <T> List<T> loadPaginatedData(int batchSize, PaginationResourceSupplier<T> resourceSupplier) {
    int currBatchSize;
    var offset = 0;
    var allUsers = new ArrayList<T>();

    do {
      var list = resourceSupplier.load(offset, batchSize);
      currBatchSize = list.size();
      offset += currBatchSize;
      allUsers.addAll(list);
    } while (currBatchSize > 0);

    return allUsers;
  }

  public interface PaginationResourceSupplier<T> {

    List<T> load(int offset, int limit);
  }
}
