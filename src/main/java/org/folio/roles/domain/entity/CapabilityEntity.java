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
@Table(name = "capability")
@EqualsAndHashCode(callSuper = true)
public class CapabilityEntity extends Auditable {

  public static final Sort DEFAULT_CAPABILITY_SORT = Sort.by(Direction.ASC, "name");

  @Id
  @FolioUuidGenerator
  private UUID id;

  @Column(name = "name", unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "resource", nullable = false)
  private String resource;

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

  @Column(name = "folio_permission", nullable = false)
  private String permission;

  @Fetch(FetchMode.SUBSELECT)
  @ElementCollection(fetch = EAGER, targetClass = EmbeddableEndpoint.class)
  @CollectionTable(name = "capability_endpoint", joinColumns = @JoinColumn(name = "capability_id"))
  private List<EmbeddableEndpoint> endpoints;
}
