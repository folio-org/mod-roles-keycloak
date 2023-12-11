package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class for role information.
 */
@Data
@Entity
@Table(name = "role")
@EqualsAndHashCode(callSuper = true)
public class RoleEntity extends Auditable {

  /**
   * The unique identifier for the role.
   */
  @Id
  private UUID id;

  /**
   * The name of role.
   */
  @Column(name = "name", nullable = false)
  private String name;

  /**
   * The description of role.
   */
  @Column(name = "description", nullable = false)
  private String description;
}
