package org.folio.roles.domain.entity;

import static org.springframework.data.domain.Sort.Direction.ASC;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.entity.key.UserCapabilitySetKey;
import org.springframework.data.domain.Sort;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_capability_set")
@IdClass(UserCapabilitySetKey.class)
@EqualsAndHashCode(callSuper = true)
public class UserCapabilitySetEntity extends Auditable implements Serializable {

  public static final Sort DEFAULT_USER_CAPABILITY_SET_SORT = Sort.by(ASC, "userId", "capabilitySetId");

  @Serial private static final long serialVersionUID = 5042102138220312886L;

  /**
   * Role identifier.
   */
  @Id
  @Column(name = "user_id")
  private UUID userId;

  /**
   * Capability identifier.
   */
  @Id
  @Column(name = "capability_set_id")
  private UUID capabilitySetId;

  public static UserCapabilitySetEntity of(UUID userId, UUID capabilitySetId) {
    var result = new UserCapabilitySetEntity();
    result.setUserId(userId);
    result.setCapabilitySetId(capabilitySetId);
    return result;
  }
}
