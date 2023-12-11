package org.folio.roles.service;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.roles.service.reference.PoliciesDataLoader;
import org.folio.roles.service.reference.RolesDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @Mock
  private RolesDataLoader rolesDataLoader;
  @Mock
  private PoliciesDataLoader policiesDataLoader;
  @Mock
  private JdbcTemplate jdbcTemplate;
  @Mock
  private FolioSpringLiquibase folioSpringLiquibase;
  @Mock
  private KafkaAdminService kafkaAdminService;
  @Mock
  private FolioExecutionContext context;
  private CustomTenantService customTenantService;

  @BeforeEach
  void setUp() {
    var referenceDataLoader = of(rolesDataLoader, policiesDataLoader);
    customTenantService = new CustomTenantService(jdbcTemplate, context, folioSpringLiquibase, kafkaAdminService,
      referenceDataLoader);
  }

  @Test
  void loadReferenceData_positive() {
    doNothing().when(rolesDataLoader).loadReferenceData();
    doNothing().when(policiesDataLoader).loadReferenceData();

    customTenantService.loadReferenceData();

    verify(rolesDataLoader).loadReferenceData();
    verify(policiesDataLoader).loadReferenceData();
  }

  @Test
  void loadReferenceData_negative_if_error() {
    doNothing().when(rolesDataLoader).loadReferenceData();
    doThrow(RuntimeException.class).when(policiesDataLoader).loadReferenceData();

    assertThatThrownBy(() -> customTenantService.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Unable to load reference data");

    verify(rolesDataLoader).loadReferenceData();
    verify(policiesDataLoader).loadReferenceData();
  }
}
