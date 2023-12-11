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
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.springframework.data.domain.Sort;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "role_capability")
@IdClass(RoleCapabilityKey.class)
@EqualsAndHashCode(callSuper = true)
public class RoleCapabilityEntity extends Auditable implements Serializable {

  public static final Sort DEFAULT_ROLE_CAPABILITY_SORT = Sort.by(ASC, "roleId", "capabilityId");

  @Serial private static final long serialVersionUID = 5042102138220312886L;

  /**
   * Role identifier.
   */
  @Id
  @Column(name = "role_id")
  private UUID roleId;

  /**
   * Capability identifier.
   */
  @Id
  @Column(name = "capability_id")
  private UUID capabilityId;
}
