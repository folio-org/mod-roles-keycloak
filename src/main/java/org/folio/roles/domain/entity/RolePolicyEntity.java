package org.folio.roles.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entity class for role-based policy information.
 */
@Data
@Entity
@DiscriminatorValue("ROLE")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RolePolicyEntity extends BasePolicyEntity {

  /**
   * The list of roles associated with this policy.
   */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "policy_roles", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "role_id")
  private List<RolePolicyRoleEntity> roles;
}
