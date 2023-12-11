package org.folio.roles.integration.keyclock.client;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.policy.BasePolicy;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A Keycloak feign client for operations with policies.
 */
@FeignClient(name = "keycloak-policies-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class)
public interface PolicyClient {

  @PostMapping("/admin/realms/{tenantId}/clients/{clientId}/authz/resource-server/policy/{policyType}")
  BasePolicy create(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                    @PathVariable String clientId, @PathVariable String policyType, @RequestBody BasePolicy request);

  @PutMapping("/admin/realms/{tenantId}/clients/{clientId}/authz/resource-server/policy/{policyType}/{policyId}")
  void updateById(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                  @PathVariable String clientId, @PathVariable String policyType, @PathVariable UUID policyId,
                  @RequestBody BasePolicy request);

  @GetMapping("/admin/realms/{tenantId}/clients/{clientId}/authz/resource-server/policy/{policyId}")
  BasePolicy getById(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                     @PathVariable String clientId, @PathVariable UUID policyId);

  @DeleteMapping("/admin/realms/{tenantId}/clients/{clientId}/authz/resource-server/policy/{policyId}")
  void deleteById(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                  @PathVariable String clientId, @PathVariable UUID policyId);

  @GetMapping(
    "/admin/realms/{tenantId}/clients/{clientId}/authz/resource-server/policy?first={first}&max={max}&name={name}"
  )
  List<BasePolicy> findAll(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                           @PathVariable String clientId, @RequestParam Integer first, @RequestParam Integer max,
                           @RequestParam String name);
}
