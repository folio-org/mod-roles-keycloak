package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.entity.CapabilityEndpointEntity.CapabilityEndpointPrimaryKey;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "capability_endpoint")
@IdClass(CapabilityEndpointPrimaryKey.class)
public class CapabilityEndpointEntity {

  @Id
  @Column(name = "capability_id")
  private UUID capabilityId;

  @Id
  private String path;

  @Id
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private HttpMethod method;

  @Data
  public static class CapabilityEndpointPrimaryKey {

    private UUID capabilityId;
    private String path;
    private HttpMethod method;
  }
}
