package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity class for role information.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "role")
@Inheritance(strategy = InheritanceType.JOINED)
public class RoleEntity extends Auditable implements Identifiable<UUID> {

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

  /**
   * The type of role.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", columnDefinition = "role_type")
  private EntityRoleType type;
}
