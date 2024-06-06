package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.CapabilityEndpointEntity;
import org.folio.roles.repository.CapabilityEndpointRepository;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityEndpointServiceTest {

  @InjectMocks private CapabilityEndpointService capabilityEndpointService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilityEndpointRepository endpointRepository;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("getByCapabilityIds")
  class GetByCapabilityIds {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();

    @Test
    void positive() {
      var endpoint1 = endpoint("/c2", GET);
      var endpoint2 = endpoint("/c1", GET);
      var capability1 = capability(capabilityId1, endpoint1);
      var capability2 = capability(capabilityId2, endpoint2);
      when(capabilityService.findByIds(List.of(capabilityId1))).thenReturn(List.of(capability1));
      when(capabilityService.findByIds(List.of(capabilityId2))).thenReturn(List.of(capability2));

      var result = capabilityEndpointService.getByCapabilityIds(List.of(capabilityId1), List.of(capabilityId2));

      assertThat(result).containsExactly(endpoint1);
    }

    @Test
    void positive_allEndpointsAssigned() {
      var endpoint1 = endpoint("/c2", GET);
      var endpoint2 = endpoint("/c1", GET);
      var capability1 = capability(capabilityId1, endpoint1);
      var capability2 = capability(capabilityId2, endpoint1, endpoint2);
      when(capabilityService.findByIds(List.of(capabilityId1))).thenReturn(List.of(capability1));
      when(capabilityService.findByIds(List.of(capabilityId2))).thenReturn(List.of(capability2));

      var result = capabilityEndpointService.getByCapabilityIds(List.of(capabilityId1), List.of(capabilityId2));

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getByCapabilitySetIds")
  class GetByCapabilitySetIds {

    private final UUID csId1 = UUID.randomUUID();
    private final UUID csId2 = UUID.randomUUID();
    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();

    @Test
    void positive() {
      var endpoint1 = endpoint("/c2", GET);
      var endpoint2 = endpoint("/c1", GET);
      var capability1 = capability(capabilityId1, endpoint1);
      var capability2 = capability(capabilityId2, endpoint2);
      when(capabilityService.findByCapabilitySetIds(List.of(csId1))).thenReturn(List.of(capability1));
      when(capabilityService.findByCapabilitySetIds(List.of(csId2))).thenReturn(List.of(capability2));

      var result = capabilityEndpointService.getByCapabilitySetIds(List.of(csId1), List.of(csId2), emptyList());

      assertThat(result).containsExactly(endpoint1);
    }

    @Test
    void positive_endpointExcluded() {
      var endpoint1 = endpoint("/c2", GET);
      var endpoint2 = endpoint("/c1", GET);
      var capability1 = capability(capabilityId1, endpoint1);
      var capability2 = capability(capabilityId2, endpoint2);
      when(capabilityService.findByCapabilitySetIds(List.of(csId1))).thenReturn(List.of(capability1));
      when(capabilityService.findByCapabilitySetIds(List.of(csId2))).thenReturn(List.of(capability2));

      var result = capabilityEndpointService.getByCapabilitySetIds(List.of(csId1), List.of(csId2), List.of(endpoint1));

      assertThat(result).isEmpty();
    }

    @Test
    void positive_allEndpointsAssigned() {
      var endpoint1 = endpoint("/c2", GET);
      var endpoint2 = endpoint("/c1", GET);
      var capability1 = capability(capabilityId1, endpoint1);
      var capability2 = capability(capabilityId2, endpoint1, endpoint2);
      when(capabilityService.findByCapabilitySetIds(List.of(csId1))).thenReturn(List.of(capability1));
      when(capabilityService.findByCapabilitySetIds(List.of(csId2))).thenReturn(List.of(capability2));

      var result = capabilityEndpointService.getByCapabilitySetIds(List.of(csId1), List.of(csId2), emptyList());

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getRoleAssignedEndpoints")
  class GetRoleAssignedEndpoints {

    @Test
    void positive_bothExclusionListsPresent() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      var capabilitiesString = CAPABILITY_ID.toString();
      var setString = CAPABILITY_SET_ID.toString();
      when(endpointRepository.getByRoleId(ROLE_ID, capabilitiesString)).thenReturn(capabilityEntities);
      when(endpointRepository.getByRoleId(ROLE_ID, capabilitiesString, setString)).thenReturn(setEntities);

      var result = capabilityEndpointService.getRoleAssignedEndpoints(
        ROLE_ID, List.of(CAPABILITY_ID), List.of(CAPABILITY_SET_ID));

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }

    @Test
    void positive_exclusionsListEmpty() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      when(endpointRepository.getByRoleId(ROLE_ID, null)).thenReturn(capabilityEntities);
      when(endpointRepository.getByRoleId(ROLE_ID, null, null)).thenReturn(setEntities);

      var result = capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList());

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }

    @Test
    void positive_exclusionsListNull() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      when(endpointRepository.getByRoleId(ROLE_ID, null)).thenReturn(capabilityEntities);
      when(endpointRepository.getByRoleId(ROLE_ID, null, null)).thenReturn(setEntities);

      var result = capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, null, null);

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }
  }

  @Nested
  @DisplayName("getUserAssignedEndpoints")
  class GetUserAssignedEndpoints {

    @Test
    void positive_bothExclusionListsPresent() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      var capabilitiesString = CAPABILITY_ID.toString();
      var setString = CAPABILITY_SET_ID.toString();
      when(endpointRepository.getByUserId(USER_ID, capabilitiesString)).thenReturn(capabilityEntities);
      when(endpointRepository.getByUserId(USER_ID, capabilitiesString, setString)).thenReturn(setEntities);

      var result = capabilityEndpointService.getUserAssignedEndpoints(
        USER_ID, List.of(CAPABILITY_ID), List.of(CAPABILITY_SET_ID));

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }

    @Test
    void positive_exclusionsListEmpty() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      when(endpointRepository.getByUserId(USER_ID, null)).thenReturn(capabilityEntities);
      when(endpointRepository.getByUserId(USER_ID, null, null)).thenReturn(setEntities);

      var result = capabilityEndpointService.getUserAssignedEndpoints(USER_ID, emptyList(), emptyList());

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }

    @Test
    void positive_exclusionsListNull() {
      var capabilityId1 = UUID.randomUUID();
      var capabilityId2 = UUID.randomUUID();

      var capabilityEntities = List.of(CapabilityEndpointEntity.of(capabilityId1, "/test/1", GET));
      var setEntities = List.of(CapabilityEndpointEntity.of(capabilityId2, "/test/2", GET));

      when(endpointRepository.getByUserId(USER_ID, null)).thenReturn(capabilityEntities);
      when(endpointRepository.getByUserId(USER_ID, null, null)).thenReturn(setEntities);

      var result = capabilityEndpointService.getUserAssignedEndpoints(USER_ID, null, null);

      assertThat(result).containsExactly(endpoint("/test/1", GET), endpoint("/test/2", GET));
    }
  }
}
