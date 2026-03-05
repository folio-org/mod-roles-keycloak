package org.folio.roles.service;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationClientProvider;
import org.folio.roles.integration.keyclock.KeycloakClientService;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.service.migration.CapabilitiesMergeService;
import org.folio.roles.service.reference.ReferenceDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class CustomTenantService extends TenantService {

  private final List<ReferenceDataLoader> referenceDataLoaders;
  private final LoadableRoleService loadableRoleService;
  private final Keycloak keycloak;
  private final CapabilitiesMergeService capabilitiesMergeService;
  private final KeycloakClientService keycloakClientService;
  private final KeycloakAuthorizationClientProvider authorizationClientProvider;
  private final FolioExecutionContext folioExecutionContext;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase, List<ReferenceDataLoader> referenceDataLoaders,
    LoadableRoleService loadableRoleService, Keycloak keycloak, CapabilitiesMergeService capabilitiesMergeService,
    KeycloakClientService keycloakClientService,
    KeycloakAuthorizationClientProvider authorizationClientProvider) {
  
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.referenceDataLoaders = referenceDataLoaders;
    this.loadableRoleService = loadableRoleService;
    this.keycloak = keycloak;
    this.capabilitiesMergeService = capabilitiesMergeService;
    this.keycloakClientService = keycloakClientService;
    this.authorizationClientProvider = authorizationClientProvider;
    this.folioExecutionContext = context;
  }

  @Override
  public void loadReferenceData() {
    log.info("Loading reference data");
    toStream(referenceDataLoaders).forEach(ReferenceDataLoader::loadReferenceData);
    log.info("Finished loading reference data");
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("Issuing fresh Keycloak token after tenant update");
    keycloak.tokenManager().grantToken();
    log.debug("Merging duplicate capabilities after tenant update");
    capabilitiesMergeService.mergeDuplicateCapabilities();
  }

  @Override
  public void deleteTenant(TenantAttributes tenantAttributes) {
    if (tenantExists() && tenantAttributes.getPurge().equals(Boolean.TRUE)) {
      try {
        loadableRoleService.cleanupDefaultRolesFromKeycloak();
      } catch (Exception e) {
        log.warn("Unable to delete all Default Roles in Keycloak. "
          + "Tenant data will be purged from the DB anyway. "
          + "Data consistency should be checked manually.", e);
      } finally {
        var tenantId = folioExecutionContext.getTenantId();
        keycloakClientService.evictLoginClient(tenantId);
        authorizationClientProvider.evictAuthorizationClient(tenantId);
        super.deleteTenant(tenantAttributes);
      }
    }
  }
}
