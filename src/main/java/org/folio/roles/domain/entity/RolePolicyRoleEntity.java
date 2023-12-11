package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.Data;

/**
 * Embeddable entity class for role-policy relationship information.
 */
@Data
@Embeddable
public class RolePolicyRoleEntity {

  /**
   * The ID of the role.
   */
  @Column(name = "role_id")
  private UUID id;

  /**
   * Indicates if the role is required for this policy.
   */
  @Column(name = "required")
  private Boolean required;
}
