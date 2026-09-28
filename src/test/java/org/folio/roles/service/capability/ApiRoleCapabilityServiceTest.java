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
import org.instancio.junit.Given;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void create_positive(@Given UUID roleId, @Given List<UUID> capabilityIds,
      @Given PageResult<RoleCapability> result) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      when(delegate.create(roleId, capabilityIds, false)).thenReturn(result);

      var actual = service.create(roleId, capabilityIds, false);

      assertThat(actual).isEqualTo(result);
    }

    @Test
    void create_negative_roleIsDefault(@Given UUID roleId, @Given List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.create(roleId, capabilityIds, false))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @Test
    void update_positive(@Given UUID roleId, @Given List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.update(roleId, capabilityIds);
      verify(delegate).update(roleId, capabilityIds);
    }

    @Test
    void update_negative_roleIsDefault(@Given UUID roleId, @Given List<UUID> capabilityIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.update(roleId, capabilityIds))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @Test
    void deleteAll_positive(@Given UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.deleteAll(roleId);
      verify(delegate).deleteAll(roleId);
    }

    @Test
    void delete_positive(@Given UUID roleId, @Given UUID capabilityId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      service.delete(roleId, capabilityId);
      verify(delegate).delete(roleId, capabilityId);
    }

    @Test
    void deleteAll_negative_roleIsDefault(@Given UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.deleteAll(roleId))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }
  }

  @Nested
  class UnProtectedMethods {

    @Test
    void find_positive(@Given String query, @Given Integer limit, @Given Integer offset,
      @Given PageResult<RoleCapability> result) {
      when(delegate.find(query, limit, offset)).thenReturn(result);

      var actual = service.find(query, limit, offset);
      assertThat(actual).isEqualTo(result);
    }

    @Test
    void getCapabilitySetCapabilityIds_positive(@Given UUID roleId, @Given List<UUID> result) {
      when(delegate.getCapabilitySetCapabilityIds(roleId)).thenReturn(result);

      var actual = service.getCapabilitySetCapabilityIds(roleId);
      assertThat(actual).isEqualTo(result);
    }
  }
}
