package org.folio.roles.domain.entity;

import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.roles.domain.entity.type.EntityCapabilityAction;
import org.folio.roles.domain.entity.type.EntityCapabilityType;
import org.folio.roles.repository.generators.FolioUuidGenerator;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Data
@Entity
@Table(name = "capability_set")
@EqualsAndHashCode(callSuper = true)
public class CapabilitySetEntity extends Auditable {

  public static final Sort DEFAULT_CAPABILITY_SET_SORT = Sort.by(Direction.ASC, "name");

  @Id
  @FolioUuidGenerator
  private UUID id;

  @Column(name = "name", unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "resource", nullable = false)
  private String resource;

  @Column(name = "module_id")
  private String moduleId;

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "action", columnDefinition = "capability_action_type")
  private EntityCapabilityAction action;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", columnDefinition = "capability_type")
  private EntityCapabilityType type;

  @Column(name = "folio_permission")
  private String permission;

  @Fetch(FetchMode.SUBSELECT)
  @ElementCollection(fetch = EAGER)
  @CollectionTable(name = "capability_set_capability", joinColumns = @JoinColumn(name = "capability_set_id"))
  @Column(name = "capability_id", nullable = false)
  private List<UUID> capabilities;

  @Column(name = "visible")
  private Boolean visible = false;
}
