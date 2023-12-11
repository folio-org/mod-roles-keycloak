package org.folio.roles.service;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.roles.service.reference.ReferenceDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class CustomTenantService extends TenantService {

  private final KafkaAdminService kafkaAdminService;
  private final List<ReferenceDataLoader> referenceDataLoaders;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
                             FolioSpringLiquibase folioSpringLiquibase, KafkaAdminService kafkaAdminService,
                             List<ReferenceDataLoader> referenceDataLoaders) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaAdminService = kafkaAdminService;
    this.referenceDataLoaders = referenceDataLoaders;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("Restarting event listeners after tenant update");
    kafkaAdminService.restartEventListeners();
  }

  @Override
  public void loadReferenceData() {
    try {
      log.info("Loading reference data");
      toStream(referenceDataLoaders).forEach(ReferenceDataLoader::loadReferenceData);
    } catch (Exception e) {
      log.warn("Unable to load reference data", e);
      throw new IllegalStateException("Unable to load reference data", e);
    }
    log.info("Finished loading reference data");
  }
}
