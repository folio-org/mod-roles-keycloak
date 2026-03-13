package org.folio.roles.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationClientProvider;
import org.folio.roles.integration.keyclock.KeycloakClientService;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.service.migration.CapabilitiesMergeService;
import org.folio.roles.service.reference.PoliciesDataLoader;
import org.folio.roles.service.reference.ReferenceDataLoader;
import org.folio.roles.service.reference.RolesDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenManager;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  private static final String TENANT_ID = "test-tenant";

  @Mock
  private RolesDataLoader rolesDataLoader;
  @Mock
  private PoliciesDataLoader policiesDataLoader;
  @Mock
  private JdbcTemplate jdbcTemplate;
  @Mock
  private FolioSpringLiquibase folioSpringLiquibase;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private LoadableRoleService loadableRoleService;
  @Mock
  private Keycloak keycloak;
  @Mock
  private CapabilitiesMergeService capabilitiesMergeService;
  @Mock
  private KafkaAdminService kafkaAdminService;
  @Mock
  private KeycloakClientService keycloakClientService;
  @Mock
  private KeycloakAuthorizationClientProvider authorizationClientProvider;
  private CustomTenantService customTenantService;

  @BeforeEach
  void setUp() {
    var referenceDataLoader = List.of(rolesDataLoader, policiesDataLoader);
    customTenantService = new TestCustomTenantService(jdbcTemplate, context, folioSpringLiquibase,
      kafkaAdminService, referenceDataLoader, loadableRoleService, keycloak, capabilitiesMergeService,
      keycloakClientService, authorizationClientProvider);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(kafkaAdminService, loadableRoleService, capabilitiesMergeService,
      keycloakClientService, authorizationClientProvider);
  }

  @Test
  void loadReferenceData_positive() {
    customTenantService.loadReferenceData();

    InOrder inOrder = inOrder(kafkaAdminService, rolesDataLoader, policiesDataLoader);
    inOrder.verify(kafkaAdminService).stopKafkaListeners();
    inOrder.verify(rolesDataLoader).loadReferenceData();
    inOrder.verify(policiesDataLoader).loadReferenceData();
    inOrder.verify(kafkaAdminService).startKafkaListeners();
  }

  @Test
  void loadReferenceData_negative_exceptionWrappedAsIllegalState() {
    doThrow(RuntimeException.class).when(policiesDataLoader).loadReferenceData();

    assertThatThrownBy(() -> customTenantService.loadReferenceData())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unable to load reference data")
      .hasCauseInstanceOf(RuntimeException.class);

    verify(rolesDataLoader).loadReferenceData();
    verify(policiesDataLoader).loadReferenceData();

    InOrder inOrder = inOrder(kafkaAdminService);
    inOrder.verify(kafkaAdminService).stopKafkaListeners();
    inOrder.verify(kafkaAdminService).startKafkaListeners();
  }

  @Test
  void deleteTenant_positive_keycloakClientsEvicted() {
    var attributes = new TenantAttributes();
    attributes.setPurge(true);
    when(context.getTenantId()).thenReturn(TENANT_ID);

    customTenantService.deleteTenant(attributes);

    verify(loadableRoleService).cleanupDefaultRolesFromKeycloak();
    verify(keycloakClientService).evictLoginClient(TENANT_ID);
    verify(authorizationClientProvider).evictAuthorizationClient(TENANT_ID);
  }

  @Test
  void deleteTenant_positive_purgeDisabled_noop() {
    var attributes = new TenantAttributes();
    attributes.setPurge(false);

    customTenantService.deleteTenant(attributes);

    verifyNoInteractions(loadableRoleService);
  }

  @Test
  void deleteTenant_positive_cleanupFails_keycloakClientsStillEvicted() {
    var attributes = new TenantAttributes();
    attributes.setPurge(true);
    when(context.getTenantId()).thenReturn(TENANT_ID);
    doThrow(new RuntimeException()).when(loadableRoleService).cleanupDefaultRolesFromKeycloak();

    customTenantService.deleteTenant(attributes);

    verify(loadableRoleService).cleanupDefaultRolesFromKeycloak();
    verify(keycloakClientService).evictLoginClient(TENANT_ID);
    verify(authorizationClientProvider).evictAuthorizationClient(TENANT_ID);
  }

  @Test
  void afterTenantUpdate_positive_kafkaRestartedAndKeycloakTokenRefreshed() {
    var tokenManager = mock(TokenManager.class);
    when(keycloak.tokenManager()).thenReturn(tokenManager);

    var attributes = new TenantAttributes();
    customTenantService.afterTenantUpdate(attributes);

    verify(kafkaAdminService).restartEventListeners();
    verify(tokenManager).grantToken();
    verify(capabilitiesMergeService).mergeDuplicateCapabilities();
  }

  public static class TestCustomTenantService extends CustomTenantService {

    TestCustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase, KafkaAdminService kafkaAdminService,
      List<ReferenceDataLoader> referenceDataLoaders,
      LoadableRoleService loadableRoleService, Keycloak keycloak, CapabilitiesMergeService capabilitiesMergeService,
      KeycloakClientService keycloakClientService,
      KeycloakAuthorizationClientProvider authorizationClientProvider) {

      super(jdbcTemplate, context, folioSpringLiquibase, kafkaAdminService,
        referenceDataLoaders, loadableRoleService,
        keycloak, capabilitiesMergeService, keycloakClientService, authorizationClientProvider);
    }

    @Override
    public boolean tenantExists() {
      return true;
    }

    @Override
    public String getSchemaName() {
      return "test";
    }
  }
}
