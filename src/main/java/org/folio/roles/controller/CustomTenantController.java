package org.folio.roles.controller;

import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custom tenant controller that suspends Kafka listeners during tenant operations.
 *
 * <p>This controller extends {@link TenantController} to wrap tenant API calls with Kafka listener
 * stop/start operations. This prevents a race condition between Kafka event processing and Liquibase database
 * migrations during tenant initialization.</p>
 *
 * <p>The stop/start pattern ensures that:
 * <ul>
 *   <li>Kafka listeners are stopped before any tenant operation begins</li>
 *   <li>Kafka listeners are always restarted after the operation completes (via finally block)</li>
 *   <li>Events buffered in Kafka during suspension are processed after restart</li>
 * </ul>
 * </p>
 *
 * @see KafkaAdminService#stopKafkaListeners()
 * @see KafkaAdminService#startKafkaListeners()
 */
@Log4j2
@RestController("folioTenantController")
public class CustomTenantController extends TenantController {

  private final KafkaAdminService kafkaAdminService;

  public CustomTenantController(TenantService tenantService, KafkaAdminService kafkaAdminService) {
    super(tenantService);
    this.kafkaAdminService = kafkaAdminService;
  }

  @Override
  public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
    try {
      log.debug("Stopping Kafka listeners before tenant creation/update");
      kafkaAdminService.stopKafkaListeners();
      log.debug("Processing tenant creation/update");
      return super.postTenant(tenantAttributes);
    } finally {
      try {
        kafkaAdminService.startKafkaListeners();
      } catch (Exception e) {
        log.error("Failed to restart Kafka listeners after tenant operation - manual intervention may be required", e);
      }
    }
  }
}
