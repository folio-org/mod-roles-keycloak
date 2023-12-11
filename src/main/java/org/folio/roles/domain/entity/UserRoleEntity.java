package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.roles.domain.entity.key.UserRoleKey;

@Data
@Entity
@Table(name = "user_role")
@EqualsAndHashCode(callSuper = true)
@IdClass(UserRoleKey.class)
public class UserRoleEntity extends Auditable {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Id
  @Column(name = "role_id")
  private UUID roleId;
}
