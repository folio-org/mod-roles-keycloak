package org.folio.roles.integration.keyclock.model.policy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import lombok.Data;
import org.folio.roles.domain.dto.SourceType;

@Data
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "type",
  visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = UserPolicy.class, name = "user"),
  @JsonSubTypes.Type(value = RolePolicy.class, name = "role"),
  @JsonSubTypes.Type(value = TimePolicy.class, name = "time"),
})
public abstract class BasePolicy {

  private UUID id;
  private String name;
  private String description;
  private String logic;
  private String type;
  private String decisionStrategy;
  private SourceType source;
}
