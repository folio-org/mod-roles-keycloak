package org.folio.roles.integration.permissions;

import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "perms")
public interface PermissionsClient {

  @GetExchange("/users/{userId}/permissions")
  Optional<Permissions> getUserPermissions(
    @PathVariable("userId") UUID userId,
    @RequestParam("indexField") String indexField,
    @RequestParam("expanded") boolean expanded);
}
