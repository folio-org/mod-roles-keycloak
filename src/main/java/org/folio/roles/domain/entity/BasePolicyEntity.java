package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.roles.domain.model.LogicType;
import org.folio.roles.repository.generators.FolioUuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A JPA entity class that represents a base policy.
 */
@Data
@Entity(name = "policy")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public class BasePolicyEntity extends Auditable {

  /**
   * The ID of the policy.
   */
  @Id
  @FolioUuidGenerator
  @Column(name = "id")
  private UUID id;

  /**
   * The name of the policy.
   */
  @Column(name = "name", nullable = false)
  private String name;

  /**
   * The description of the policy.
   */
  @Column(name = "description")
  private String description;

  /**
   * Flag that shows is it a system created policy.
   */
  @Column(name = "is_system", updatable = false)
  private Boolean system = false;

  /**
   * The logic of the policy.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "logic", columnDefinition = "logic_type", insertable = false, updatable = false)
  private LogicType logic;
}
