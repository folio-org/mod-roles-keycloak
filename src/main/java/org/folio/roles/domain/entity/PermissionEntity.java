package org.folio.roles.domain.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.folio.roles.repository.generators.FolioUuidGenerator;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "permission")
public class PermissionEntity {

  @Id
  @FolioUuidGenerator
  @Column(name = "id")
  private UUID id;

  @Column(name = "name")
  private String permissionName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "description")
  private String description;

  @Column(name = "visible")
  private Boolean visible;

  @Type(ListArrayType.class)
  @Column(name = "sub_permissions", columnDefinition = "text[]")
  private List<String> subPermissions;

  @Type(ListArrayType.class)
  @Column(name = "replaces", columnDefinition = "text[]")
  private List<String> replaces;
}
