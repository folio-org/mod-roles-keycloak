package org.folio.roles.integration.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.folio.roles.domain.model.CapabilityReplacements;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityReplacementsService;
import org.folio.roles.service.capability.UserPermissionsCacheEvictor;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  private static final String MODULE_ID = "test-module-1.0.0";

  @InjectMocks private KafkaMessageListener kafkaMessageListener;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private CapabilityKafkaEventHandler capabilityKafkaEventHandler;
  @Mock private CapabilityReplacementsService capabilityReplacementsService;
  @Mock private SystemUserScopedExecutionService systemUserScopedExecutionService;
  @Mock private ExecutionContextBuilder executionContextBuilder;
  @Mock private UserPermissionsCacheEvictor userPermissionsCacheEvictor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(folioModuleMetadata, capabilityKafkaEventHandler, userPermissionsCacheEvictor);
  }

  @BeforeEach
  void setUp() {
    when(executionContextBuilder.buildContext(TENANT_ID))
      .thenReturn(mock(FolioExecutionContext.class));
  }

  @Test
  void handleCapabilityEvent_positive_evictsUserPermissionsCache() {
    givenSystemUserScopedExecutionRunsCallable();
    var resourceEvent = resourceEvent();

    kafkaMessageListener.handleCapabilityEvent(resourceEvent);

    // Assert
    verify(capabilityKafkaEventHandler).handleEvent(resourceEvent);
    verify(userPermissionsCacheEvictor).evictUserPermissionsForCurrentTenant();
    verifyNoInteractions(capabilityReplacementsService);
  }

  @Test
  void handleCapabilityEvent_negative_replacementsProcessingFails_evictsUserPermissionsCache() {
    givenSystemUserScopedExecutionRunsCallable();
    var resourceEvent = resourceEvent();
    var replacements = mock(CapabilityReplacements.class);
    when(capabilityKafkaEventHandler.handleEvent(resourceEvent))
      .thenReturn(Optional.of(replacements));
    doThrow(new RuntimeException("boom")).when(capabilityReplacementsService)
      .processReplacements(replacements);

    assertThatThrownBy(() -> kafkaMessageListener.handleCapabilityEvent(resourceEvent))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("boom");

    verify(userPermissionsCacheEvictor).evictUserPermissionsForCurrentTenant();
  }

  @Test
  void handleCapabilityEvent_negative_handlerThrows_evictsUserPermissionsCache() {
    givenSystemUserScopedExecutionRunsCallable();
    var resourceEvent = resourceEvent();
    when(capabilityKafkaEventHandler.handleEvent(resourceEvent))
      .thenThrow(new RuntimeException("error"));

    // Act + Assert
    assertThatThrownBy(() -> kafkaMessageListener.handleCapabilityEvent(resourceEvent))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("error");

    verify(userPermissionsCacheEvictor).evictUserPermissionsForCurrentTenant();
    verifyNoInteractions(capabilityReplacementsService);
  }

  @Test
  void handleCapabilityEvent_negative_executeSystemUserScopedThrowsBeforeCallableRuns_evictsUserPermissionsCache() {
    var resourceEvent = resourceEvent();
    when(systemUserScopedExecutionService.executeSystemUserScoped(any()))
      .thenThrow(new RuntimeException("boom"));

    // Act + Assert
    assertThatThrownBy(() -> kafkaMessageListener.handleCapabilityEvent(resourceEvent))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("boom");

    verifyNoInteractions(capabilityKafkaEventHandler, capabilityReplacementsService);
    verify(userPermissionsCacheEvictor).evictUserPermissionsForCurrentTenant();
  }

  private void givenSystemUserScopedExecutionRunsCallable() {
    when(systemUserScopedExecutionService.executeSystemUserScoped(any())).thenAnswer(inv -> {
      Callable<?> callable = inv.getArgument(0);
      callable.call();
      return null;
    });
  }

  private static ResourceEvent resourceEvent() {
    return ResourceEvent.builder()
      .tenant(TENANT_ID)
      .newValue(capabilityEventBodyAsMap())
      .build();
  }

  private static Map<String, Object> capabilityEventBodyAsMap() {
    return Map.of(
      "moduleId", MODULE_ID,
      "moduleType", "module",
      "applicationId", APPLICATION_ID,
      "resources", List.of(Map.of(
        "permission", Map.of(
          "permissionName", "test-resource.item.get",
          "displayName", "Test-Resource Item - Get by ID",
          "description", "Get test-resource item by id"),
        "endpoints", List.of(Map.of("path", "/test-items/{id}", "method", "GET")))
      ));
  }
}
