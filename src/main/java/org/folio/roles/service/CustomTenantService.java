package org.folio.roles.service;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.roles.service.loadablerole.LoadableRoleService;
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

  private final KafkaAdminService kafkaAdminService;
  private final List<ReferenceDataLoader> referenceDataLoaders;
  private final LoadableRoleService loadableRoleService;
  private final Keycloak keycloak;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase, KafkaAdminService kafkaAdminService,
    List<ReferenceDataLoader> referenceDataLoaders, LoadableRoleService loadableRoleService, Keycloak keycloak) {

    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaAdminService = kafkaAdminService;
    this.referenceDataLoaders = referenceDataLoaders;
    this.loadableRoleService = loadableRoleService;
    this.keycloak = keycloak;
  }

  @Override
  public void loadReferenceData() {
    kafkaAdminService.stopKafkaListeners();
    try {
      log.info("Loading reference data");
      toStream(referenceDataLoaders).forEach(ReferenceDataLoader::loadReferenceData);
    } catch (Exception e) {
      log.warn("Unable to load reference data", e);
      throw new IllegalStateException("Unable to load reference data", e);
    } finally {
      kafkaAdminService.startKafkaListeners();
    }
    log.info("Finished loading reference data");
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("Restarting event listeners after tenant update");
    kafkaAdminService.restartEventListeners();
    log.debug("Issuing fresh Keycloak token after tenant update");
    keycloak.tokenManager().grantToken();
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
        super.deleteTenant(tenantAttributes);
      }
    }
  }
}
