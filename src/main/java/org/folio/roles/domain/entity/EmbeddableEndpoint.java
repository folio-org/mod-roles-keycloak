package org.folio.roles.domain.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.folio.roles.domain.dto.HttpMethod;
import org.hibernate.annotations.Type;

@Data
@Embeddable
public class EmbeddableEndpoint {

  private String path;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  private HttpMethod method;
}
