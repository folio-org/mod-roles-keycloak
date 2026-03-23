package org.folio.roles.integration.mte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.roles.integration.mte.model.MteApplicationDescriptor;
import org.folio.roles.integration.mte.model.MteApplicationDescriptors;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MteEntitlementServiceTest {

  @Mock private TenantEntitlementsClient client;
  @Mock private FolioExecutionContext folioExecutionContext;
  @InjectMocks private MteEntitlementService service;

  @Test
  void getEntitledApplicationIds_positive_extractsIds() {
    when(folioExecutionContext.getTenantId()).thenReturn("test");
    when(folioExecutionContext.getToken()).thenReturn("token");
    when(client.findEntitledApplicationsByTenantName("test", "token", "test", 500, 0))
      .thenReturn(MteApplicationDescriptors.builder()
        .applicationDescriptors(List.of(MteApplicationDescriptor.builder().id("app-a-1.0.0").build()))
        .totalRecords(1)
        .build());

    var result = service.getEntitledApplicationIdsForCurrentTenant();
    assertThat(result).containsExactly("app-a-1.0.0");
  }

  @Test
  void getEntitledApplicationIds_positive_nullIdIsFiltered() {
    when(folioExecutionContext.getTenantId()).thenReturn("test");
    when(folioExecutionContext.getToken()).thenReturn("token");
    when(client.findEntitledApplicationsByTenantName("test", "token", "test", 500, 0))
      .thenReturn(MteApplicationDescriptors.builder()
        .applicationDescriptors(List.of(MteApplicationDescriptor.builder().build()))
        .totalRecords(1)
        .build());

    var result = service.getEntitledApplicationIdsForCurrentTenant();
    assertThat(result).isEmpty();
  }

  @Test
  void getEntitledApplicationIds_positive_emptyDescriptorsList() {
    when(folioExecutionContext.getTenantId()).thenReturn("test");
    when(folioExecutionContext.getToken()).thenReturn("token");
    when(client.findEntitledApplicationsByTenantName("test", "token", "test", 500, 0))
      .thenReturn(MteApplicationDescriptors.builder().applicationDescriptors(List.of()).totalRecords(0).build());

    var result = service.getEntitledApplicationIdsForCurrentTenant();
    assertThat(result).isEmpty();
  }

  @Test
  void getEntitledApplicationIds_negative_propagatesClientFailure() {
    when(folioExecutionContext.getTenantId()).thenReturn("test");
    when(folioExecutionContext.getToken()).thenReturn("token");
    when(client.findEntitledApplicationsByTenantName("test", "token", "test", 500, 0))
      .thenThrow(new RuntimeException("mte unavailable"));

    assertThatThrownBy(() -> service.getEntitledApplicationIdsForCurrentTenant())
      .isInstanceOf(RuntimeException.class)
      .hasMessage("mte unavailable");
  }
}
