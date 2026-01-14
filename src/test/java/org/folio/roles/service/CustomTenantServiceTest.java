package org.folio.roles.service;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.service.migration.CapabilitiesMergeService;
import org.folio.roles.service.reference.PoliciesDataLoader;
import org.folio.roles.service.reference.ReferenceDataLoader;
import org.folio.roles.service.reference.RolesDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenManager;
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
  private FolioExecutionContext context;
  @Mock
  private LoadableRoleService loadableRoleService;
  @Mock
  private Keycloak keycloak;
  @Mock
  private CapabilitiesMergeService capabilitiesMergeService;
  private CustomTenantService customTenantService;

  @BeforeEach
  void setUp() {
    var referenceDataLoader = of(rolesDataLoader, policiesDataLoader);
    customTenantService = new TestCustomTenantService(jdbcTemplate, context, folioSpringLiquibase,
      referenceDataLoader, loadableRoleService, keycloak, capabilitiesMergeService);
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
  void loadReferenceData_negative_exceptionPropagates() {
    doNothing().when(rolesDataLoader).loadReferenceData();
    doThrow(RuntimeException.class).when(policiesDataLoader).loadReferenceData();

    assertThatThrownBy(() -> customTenantService.loadReferenceData()).isInstanceOf(RuntimeException.class);

    verify(rolesDataLoader).loadReferenceData();
    verify(policiesDataLoader).loadReferenceData();
  }

  @Test
  void deleteTenant_positive() {
    var attributes = new TenantAttributes();
    attributes.setPurge(true);
    doNothing().when(loadableRoleService).cleanupDefaultRolesFromKeycloak();

    customTenantService.deleteTenant(attributes);

    verify(loadableRoleService).cleanupDefaultRolesFromKeycloak();
  }

  @Test
  void deleteTenant_positive_notPurge() {
    var attributes = new TenantAttributes();
    attributes.setPurge(false);

    customTenantService.deleteTenant(attributes);

    verifyNoInteractions(loadableRoleService);
  }

  @Test
  void deleteTenant_positive_whenError() {
    var attributes = new TenantAttributes();
    attributes.setPurge(true);

    doThrow(new RuntimeException()).when(loadableRoleService).cleanupDefaultRolesFromKeycloak();

    customTenantService.deleteTenant(attributes);

    verify(loadableRoleService).cleanupDefaultRolesFromKeycloak();
  }

  @Test
  void afterTenantUpdate_positive() {
    var tokenManager = mock(TokenManager.class);
    when(keycloak.tokenManager()).thenReturn(tokenManager);
    doNothing().when(capabilitiesMergeService).mergeDuplicateCapabilities();

    var attributes = new TenantAttributes();
    customTenantService.afterTenantUpdate(attributes);

    verify(keycloak).tokenManager();
    verify(tokenManager).grantToken();
    verify(capabilitiesMergeService).mergeDuplicateCapabilities();
  }

  public static class TestCustomTenantService extends CustomTenantService {

    TestCustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase, List<ReferenceDataLoader> referenceDataLoaders,
      LoadableRoleService loadableRoleService, Keycloak keycloak, CapabilitiesMergeService capabilitiesMergeService) {

      super(jdbcTemplate, context, folioSpringLiquibase, referenceDataLoaders, loadableRoleService,
        keycloak, capabilitiesMergeService);
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
