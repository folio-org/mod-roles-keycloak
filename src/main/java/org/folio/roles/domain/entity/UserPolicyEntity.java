package org.folio.roles.domain.entity;

import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Entity representing the UserPolicy.
 */
@Data
@Entity
@DiscriminatorValue("USER")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserPolicyEntity extends BasePolicyEntity {

  /**
   * Collection of user IDs associated with this policy.
   */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @Fetch(FetchMode.SUBSELECT)
  @ElementCollection(fetch = EAGER)
  @CollectionTable(name = "policy_users", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "user_id")
  private List<UUID> users = new ArrayList<>();
}
