package org.folio.roles.service.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(InstancioExtension.class)
class ApiRoleCapabilityServiceTest {

  @InjectMocks private ApiRoleCapabilityService service;
  @Mock private RoleCapabilityService delegate;
  @Mock private LoadableRoleService loadableRoleService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  class ProtectedWithDefaultRoleCheckMethods {

    @ParameterizedTest
    @InstancioSource
    void create_positive(UUID roleId, List<UUID> capabilityIds, PageResult<RoleCapability> result) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      when(delegate.create(roleId, capabilityIds, false)).thenReturn(result);

      var actual = service.create(roleId, capabilityIds, false);

      assertThat(actual).isEqualTo(result);
    }

    @ParameterizedTest
    @InstancioSource
    void create_negative_roleIsDefault(UUID roleId, List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.create(roleId, capabilityIds, false))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @ParameterizedTest
    @InstancioSource
    void update_positive(UUID roleId, List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.update(roleId, capabilityIds);
      verify(delegate).update(roleId, capabilityIds);
    }

    @ParameterizedTest
    @InstancioSource
    void update_negative_roleIsDefault(UUID roleId, List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.update(roleId, capabilityIds))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @ParameterizedTest
    @InstancioSource
    void deleteAll_positive(UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.deleteAll(roleId);
      verify(delegate).deleteAll(roleId);
    }

    @ParameterizedTest
    @InstancioSource
    void delete_positive(UUID roleId, UUID capabilityId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.delete(roleId, capabilityId);
      verify(delegate).delete(roleId, capabilityId);
    }

    @ParameterizedTest
    @InstancioSource
    void deleteAll_negative_roleIsDefault(UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.deleteAll(roleId))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }
  }

  @Nested
  class UnProtectedMethods {

    @ParameterizedTest
    @InstancioSource
    void find_positive(String query, Integer limit, Integer offset, PageResult<RoleCapability> result) {
      when(delegate.find(query, limit, offset)).thenReturn(result);

      var actual = service.find(query, limit, offset);
      assertThat(actual).isEqualTo(result);
    }

    @ParameterizedTest
    @InstancioSource
    void getCapabilitySetCapabilityIds_positive(UUID roleId, List<UUID> result) {
      when(delegate.getCapabilitySetCapabilityIds(roleId)).thenReturn(result);

      var actual = service.getCapabilitySetCapabilityIds(roleId);
      assertThat(actual).isEqualTo(result);
    }
  }
}
