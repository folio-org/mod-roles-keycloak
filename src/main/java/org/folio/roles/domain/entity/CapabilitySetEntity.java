package org.folio.roles.domain.entity;

import static jakarta.persistence.FetchType.EAGER;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
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
import lombok.ToString;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilityType;
import org.folio.roles.repository.generators.FolioUuidGenerator;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
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

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(name = "action", columnDefinition = "capability_action_type")
  private CapabilityAction action;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(name = "type", columnDefinition = "capability_type")
  private CapabilityType type;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Fetch(FetchMode.SUBSELECT)
  @ElementCollection(fetch = EAGER)
  @CollectionTable(name = "capability_set_capability", joinColumns = @JoinColumn(name = "capability_set_id"))
  @Column(name = "capability_id", nullable = false)
  private List<UUID> capabilities;
}
