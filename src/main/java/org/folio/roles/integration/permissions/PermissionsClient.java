package org.folio.roles.integration.permissions;

import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "perms", dismiss404 = true)
public interface PermissionsClient {

  @GetMapping("/users/{userId}/permissions")
  Optional<Permissions> getUserPermissions(
    @PathVariable("userId") UUID userId,
    @RequestParam("indexField") String indexField,
    @RequestParam("expanded") boolean expanded);
}
