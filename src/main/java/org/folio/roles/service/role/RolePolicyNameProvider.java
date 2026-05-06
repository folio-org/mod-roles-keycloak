package org.folio.roles.service.role;

import static java.lang.String.format;

import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Endpoint;

/**
 * Centralized naming convention for the auto-generated Keycloak policy and permissions
 * associated with a role.
 *
 * <p>Both {@code RoleService} (cleanup on role deletion) and
 * {@code RolePermissionService} (creation/deletion of role permissions) must agree on these
 * names. Keeping them in one place prevents silent drift that would cause cleanup to skip
 * records it should remove.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RolePolicyNameProvider {

  /**
   * Returns the canonical name of the Keycloak policy that backs the given role.
   */
  public static String getPolicyName(UUID roleId) {
    return "Policy for role: " + roleId;
  }

  /**
   * Returns a function that, given an endpoint, produces the canonical Keycloak permission
   * name for the role/endpoint pair.
   */
  public static Function<Endpoint, String> getPermissionNameGenerator(UUID roleId) {
    return endpoint -> format("%s access for role '%s' to '%s'", endpoint.getMethod(), roleId, endpoint.getPath());
  }
}
