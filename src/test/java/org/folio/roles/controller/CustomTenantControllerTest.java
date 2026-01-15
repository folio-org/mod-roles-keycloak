package org.folio.roles.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.folio.roles.integration.kafka.KafkaAdminService;
import org.folio.roles.service.CustomTenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CustomTenantControllerTest {

  @Mock private CustomTenantService tenantService;
  @Mock private KafkaAdminService kafkaAdminService;

  @InjectMocks private CustomTenantController controller;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(kafkaAdminService);
  }

  @Test
  void postTenant_positive_stopsAndStartsKafkaListeners() {
    var attributes = tenantAttributes();
    doNothing().when(kafkaAdminService).stopKafkaListeners();
    doNothing().when(kafkaAdminService).startKafkaListeners();
    doNothing().when(tenantService).createOrUpdateTenant(attributes);

    var result = controller.postTenant(attributes);

    assertThat(result.getStatusCode()).isEqualTo(NO_CONTENT);

    var inOrder = inOrder(kafkaAdminService, tenantService);
    inOrder.verify(kafkaAdminService).stopKafkaListeners();
    inOrder.verify(tenantService).createOrUpdateTenant(attributes);
    inOrder.verify(kafkaAdminService).startKafkaListeners();
  }

  @Test
  void postTenant_positive_logsErrorOnKafkaRestartFailure() {
    var attributes = tenantAttributes();
    doNothing().when(kafkaAdminService).stopKafkaListeners();
    doThrow(new RuntimeException("Restart failed")).when(kafkaAdminService).startKafkaListeners();
    doNothing().when(tenantService).createOrUpdateTenant(attributes);

    var result = controller.postTenant(attributes);

    assertThat(result.getStatusCode()).isEqualTo(NO_CONTENT);

    var inOrder = inOrder(kafkaAdminService, tenantService);
    inOrder.verify(kafkaAdminService).stopKafkaListeners();
    inOrder.verify(tenantService).createOrUpdateTenant(attributes);
    inOrder.verify(kafkaAdminService).startKafkaListeners();
  }

  @Test
  void postTenant_negative_startsKafkaListenersOnException() {
    var attributes = tenantAttributes();
    doNothing().when(kafkaAdminService).stopKafkaListeners();
    doNothing().when(kafkaAdminService).startKafkaListeners();
    doThrow(new RuntimeException("Tenant creation failed")).when(tenantService).createOrUpdateTenant(attributes);

    assertThatThrownBy(() -> controller.postTenant(attributes))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Tenant creation failed");

    verify(kafkaAdminService).stopKafkaListeners();
    verify(kafkaAdminService).startKafkaListeners();
  }

  @Test
  void postTenant_positive_startsKafkaListenersOnTenantDelete() {
    var attributes = new TenantAttributes().moduleTo("").purge(true);
    doNothing().when(kafkaAdminService).stopKafkaListeners();
    doNothing().when(kafkaAdminService).startKafkaListeners();
    doNothing().when(tenantService).deleteTenant(attributes);

    var result = controller.postTenant(attributes);

    assertThat(result.getStatusCode()).isEqualTo(NO_CONTENT);

    var inOrder = inOrder(kafkaAdminService, tenantService);
    inOrder.verify(kafkaAdminService).stopKafkaListeners();
    inOrder.verify(tenantService).deleteTenant(attributes);
    inOrder.verify(kafkaAdminService).startKafkaListeners();
  }

  private static TenantAttributes tenantAttributes() {
    return new TenantAttributes().moduleTo("mod-roles-keycloak-1.0.0");
  }
}
