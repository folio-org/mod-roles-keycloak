package org.folio.roles.domain.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Data
@Entity
@Table(name = "role_loadable")
@EqualsAndHashCode(callSuper = true)
public class LoadableRoleEntity extends RoleEntity {

  public static final Sort DEFAULT_LOADABLE_ROLE_SORT = Sort.by(Direction.ASC, "name");

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", columnDefinition = "role_loadable_type")
  private EntityLoadableRoleType type;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Fetch(FetchMode.SUBSELECT)
  @ElementCollection(fetch = LAZY)
  @CollectionTable(name = "role_loadable_permission", joinColumns = @JoinColumn(name = "role_loadable_id"))
  @Column(name = "folio_permission", nullable = false)
  private Set<String> permissions;
}
