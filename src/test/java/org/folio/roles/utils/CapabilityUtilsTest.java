package org.folio.roles.utils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.endpoint;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.model.PageResult;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CapabilityUtilsTest {

  @Test
  void getCapabilityName_positive() {
    var result = CapabilityUtils.getCapabilityName("Test", CapabilityAction.CREATE);
    assertThat(result).isEqualTo("test.create");
  }

  @Test
  void getCapabilitySetIds_positive() {
    var capabilityId1 = UUID.randomUUID();
    var capabilityId2 = UUID.randomUUID();
    var capabilityId3 = UUID.randomUUID();
    var capabilitySets = PageResult.asSinglePage(
      capabilitySet(UUID.randomUUID(), List.of(capabilityId1, capabilityId2)),
      capabilitySet(UUID.randomUUID(), emptyList()),
      capabilitySet(UUID.randomUUID(), null),
      capabilitySet(UUID.randomUUID(), List.of(capabilityId2, capabilityId3)),
      capabilitySet(UUID.randomUUID(), List.of(capabilityId1, capabilityId3))
    );

    var result = CapabilityUtils.getCapabilitySetIds(capabilitySets);
    assertThat(result).containsExactly(capabilityId1, capabilityId2, capabilityId3);
  }

  @Test
  void getCapabilityEndpoints() {
    var endpoint1 = endpoint("/foo1", HttpMethod.GET);
    var endpoint2 = endpoint("/foo2", HttpMethod.GET);
    var endpoint3 = endpoint("/foo3", HttpMethod.GET);
    var endpoint4 = endpoint("/foo4", HttpMethod.GET);

    var capabilities = List.of(
      capability(UUID.randomUUID(), endpoint1, endpoint3),
      capability(UUID.randomUUID(), endpoint2),
      capability(UUID.randomUUID()).endpoints(emptyList()),
      capability(UUID.randomUUID()).endpoints(null),
      capability(UUID.randomUUID(), endpoint3, endpoint4));

    var result = CapabilityUtils.getCapabilityEndpoints(capabilities);
    assertThat(result).containsExactly(endpoint1, endpoint3, endpoint2, endpoint4);
  }

  @Test
  void getCapabilityNamesAsString_positive() {
    var capability1 = capability(UUID.randomUUID());
    capability1.setName("foo1.get");
    var capability2 = capability(UUID.randomUUID());
    capability2.setName("foo2.get");
    var capabilities = List.of(capability1, capability2);

    var result = CapabilityUtils.getCapabilityNamesAsString(capabilities);
    assertThat(result).isEqualTo("foo1.get, foo2.get");
  }
}
