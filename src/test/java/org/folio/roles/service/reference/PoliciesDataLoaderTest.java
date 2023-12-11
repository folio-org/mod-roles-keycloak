package org.folio.roles.service.reference;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.utils.ResourceHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PoliciesDataLoaderTest {

  @Mock
  private PolicyService policyService;
  @Mock
  private ResourceHelper resourceHelper;
  @InjectMocks
  private PoliciesDataLoader policiesDataLoader;

  @Test
  void loadReferenceData_positive_ifCreate() {
    var policy = new Policy().name("policy1");
    var policies = new Policies().policies(of(policy));

    when(resourceHelper.readObjectsFromDirectory("reference-data/policies", Policies.class))
      .thenReturn(of(policies));
    when(policyService.create(policy)).thenReturn(policy);

    policiesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/policies", Policies.class);
    verify(policyService).create(policy);
  }

  @Test
  void loadReferenceData_positive_ifUpdate() {
    var policy = new Policy().name("policy1").id(randomUUID());
    var policies = new Policies().policies(of(policy));

    when(resourceHelper.readObjectsFromDirectory("reference-data/policies", Policies.class))
      .thenReturn(of(policies));
    when(policyService.update(policy)).thenReturn(policy);
    when(policyService.existsById(policy.getId())).thenReturn(true);

    policiesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/policies", Policies.class);
    verify(policyService).update(policy);
    verify(policyService).existsById(policy.getId());
  }

  @Test
  void loadReferenceData_positive_createIfNotExist() {
    var policy = new Policy().name("policy1").id(randomUUID());
    var policies = new Policies().policies(of(policy));

    when(resourceHelper.readObjectsFromDirectory("reference-data/policies", Policies.class))
      .thenReturn(of(policies));
    when(policyService.create(policy)).thenReturn(policy);
    when(policyService.existsById(policy.getId())).thenReturn(false);

    policiesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/policies", Policies.class);
    verify(policyService).create(policy);
    verify(policyService).existsById(policy.getId());
  }

  @Test
  void loadReferenceData_negative_ifError() {
    when(resourceHelper.readObjectsFromDirectory("reference-data/policies", Policies.class))
      .thenThrow(new IllegalStateException("Failed to deserialize data"));

    assertThatThrownBy(() -> policiesDataLoader.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to deserialize data");

    verify(resourceHelper).readObjectsFromDirectory("reference-data/policies", Policies.class);
    verifyNoInteractions(policyService);
  }
}
