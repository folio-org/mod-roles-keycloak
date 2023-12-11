package org.folio.roles.integration.kafka.model;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class Permission {

  private UUID id;
  private String permissionName;
  private List<String> replaces;
  private String displayName;
  private String description;
  private List<String> subPermissions;
  private Boolean visible;

  /**
   * Sets id field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Sets permissionName field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission permissionName(String permissionName) {
    this.permissionName = permissionName;
    return this;
  }

  /**
   * Sets replaces field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission replaces(List<String> replaces) {
    this.replaces = replaces;
    return this;
  }

  /**
   * Sets displayName field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Sets description field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Sets subPermissions field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission subPermissions(List<String> subPermissions) {
    this.subPermissions = subPermissions;
    return this;
  }

  /**
   * Sets visible field and returns {@link Permission}.
   *
   * @return modified {@link Permission} value
   */
  public Permission visible(Boolean visible) {
    this.visible = visible;
    return this;
  }
}
