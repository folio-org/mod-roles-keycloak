package org.folio.roles.domain.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.HttpMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CapabilityEndpoint {

  private UUID capabilityId;
  private String path;
  private HttpMethod method;
}
