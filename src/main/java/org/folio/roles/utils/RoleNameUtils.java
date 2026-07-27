package org.folio.roles.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoleNameUtils {

  /**
   * Character that must never appear in a role name.
   *
   * <p>Keycloak serializes role-policy associations using this character as a delimiter between a client id and a
   * role name. On import the same split is applied to every entry, so a realm role containing it is misread as a
   * client role reference and the import fails.</p>
   */
  public static final String FORBIDDEN_NAME_CHARACTER = "/";

  /**
   * Role name pattern. Must be kept in sync with the {@code name} property in
   * {@code swagger.api/schemas/role/role.json}.
   */
  public static final String ROLE_NAME_PATTERN = "^[^/]+$";

  /**
   * Checks if the given role name contains characters that are not allowed in a role name.
   *
   * @param roleName - role name to check
   * @return true if the name contains a forbidden character, false otherwise
   */
  public static boolean hasForbiddenCharacters(String roleName) {
    return roleName != null && roleName.contains(FORBIDDEN_NAME_CHARACTER);
  }
}
