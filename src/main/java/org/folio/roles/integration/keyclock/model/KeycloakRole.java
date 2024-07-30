package org.folio.roles.integration.keyclock.model;

import java.util.UUID;
import lombok.Data;
import org.folio.roles.domain.dto.SourceType;

@Data
public class KeycloakRole {

  private UUID id;
  private String name;
  private String description;
  private SourceType source;
  private Boolean composite;
  private Boolean clientRole;
  private String containerId;
}
