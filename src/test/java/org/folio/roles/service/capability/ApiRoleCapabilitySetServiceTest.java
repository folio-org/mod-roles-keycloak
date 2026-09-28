package org.folio.roles.service.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.RoleCapabilitySet;
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
class ApiRoleCapabilitySetServiceTest {

  @InjectMocks private ApiRoleCapabilitySetService service;
  @Mock private RoleCapabilitySetService delegate;
  @Mock private LoadableRoleService loadableRoleService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  class ProtectedWithDefaultRoleCheckMethods {

    @Test
    void create_positive(@Given UUID roleId, @Given List<UUID> capabilitySetIds,
      @Given PageResult<RoleCapabilitySet> result) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      when(delegate.create(roleId, capabilitySetIds, false)).thenReturn(result);

      var actual = service.create(roleId, capabilitySetIds, false);

      assertThat(actual).isEqualTo(result);
    }

    @Test
    void create_negative_roleIsDefault(@Given UUID roleId, @Given List<UUID> capabilitySetIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.create(roleId, capabilitySetIds, false))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @Test
    void update_positive(@Given UUID roleId, @Given List<UUID> capabilitySetIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      doNothing().when(delegate).update(roleId, capabilitySetIds);

      service.update(roleId, capabilitySetIds);
    }

    @Test
    void update_negative_roleIsDefault(@Given UUID roleId, @Given List<UUID> capabilitySetIds) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.update(roleId, capabilitySetIds))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @Test
    void deleteAll_positive(@Given UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      doNothing().when(delegate).deleteAll(roleId);

      service.deleteAll(roleId);
    }

    @Test
    void deleteAll_negative_roleIsDefault(@Given UUID roleId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.deleteAll(roleId))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }

    @Test
    void delete_positive(@Given UUID roleId, @Given UUID capabilitySetId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(false);
      doNothing().when(delegate).delete(roleId, capabilitySetId);

      service.delete(roleId, capabilitySetId);
    }

    @Test
    void delete_negative_roleIsDefault(@Given UUID roleId, @Given UUID capabilitySetId) {
      when(loadableRoleService.isDefaultRole(roleId)).thenReturn(true);

      assertThatThrownBy(() -> service.delete(roleId, capabilitySetId))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Changes to default role are prohibited: roleId = %s", roleId);
    }
  }

  @Nested
  class UnProtectedMethods {

    @Test
    void find_positive(@Given String query, @Given Integer limit, @Given Integer offset,
      @Given PageResult<RoleCapabilitySet> result) {
      when(delegate.find(query, limit, offset)).thenReturn(result);

      var actual = service.find(query, limit, offset);
      assertThat(actual).isEqualTo(result);
    }
  }
}
