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

  /**
   * Capability identifier.
   */
  @Id
  @Column(name = "capability_id")
  private UUID capabilityId;

  /**
   * URL static path or path pattern.
   */
  @Id
  private String path;

  /**
   * HTTP method.
   */
  @Id
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private HttpMethod method;

  @Data
  public static class CapabilityEndpointPrimaryKey {

    /**
     * Capability identifier.
     */
    private UUID capabilityId;

    /**
     * URL static path or path pattern.
     */
    private String path;

    /**
     * HTTP method.
     */
    private HttpMethod method;
  }
}
