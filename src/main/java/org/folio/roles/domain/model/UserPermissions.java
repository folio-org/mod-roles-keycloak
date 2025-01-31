package org.folio.roles.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class UserPermissions {

  private UUID userId;
  private Role role;
  private String roleName;
  private List<String> permissions;
  private List<Capability> manageCapabilities = new ArrayList<>();

  /**
   * Sets userId for {@link UserPermissions} and returns {@link UserPermissions}.
   *
   * @return this {@link UserPermissions} with new userId value
   */
  public UserPermissions userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Sets role for {@link UserPermissions} and returns {@link UserPermissions}.
   *
   * @return this {@link UserPermissions} with new role value
   */
  public UserPermissions role(Role role) {
    this.role = role;
    return this;
  }

  /**
   * Sets roleName for {@link UserPermissions} and returns {@link UserPermissions}.
   *
   * @return this {@link UserPermissions} with new roleName value
   */
  public UserPermissions roleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  /**
   * Sets permissions for {@link UserPermissions} and returns {@link UserPermissions}.
   *
   * @return this {@link UserPermissions} with new permissions value
   */
  public UserPermissions permissions(List<String> permissions) {
    this.permissions = permissions;
    return this;
  }

  /**
   * Sets capabilities to manage capabilities for {@link UserPermissions} and returns {@link UserPermissions}.
   *
   * @return this {@link UserPermissions} with new capabilities value
   */
  public UserPermissions manageCapabilities(List<Capability> manageCapabilities) {
    this.manageCapabilities = manageCapabilities;
    return this;
  }
}
