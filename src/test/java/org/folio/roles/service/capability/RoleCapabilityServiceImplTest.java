package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.entity.RoleCapabilityEntity.DEFAULT_ROLE_CAPABILITY_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.domain.model.PageResult.empty;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_NAME;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapability;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.mapper.entity.RoleCapabilityEntityMapper;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.support.TestUtils;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoleCapabilityServiceImplTest {

  @InjectMocks private RoleCapabilityServiceImpl roleCapabilityService;

  @Mock private RoleService roleService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private RolePermissionService rolePermissionService;
  @Mock private RoleCapabilityRepository roleCapabilityRepository;
  @Mock private CapabilityEndpointService capabilityEndpointService;
  @Mock private RoleCapabilityEntityMapper roleCapabilityEntityMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_emptyQuery() {
      var offset = OffsetRequest.of(0, 10, DEFAULT_ROLE_CAPABILITY_SORT);
      var entity = roleCapabilityEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedRoleCapability = roleCapability();

      when(roleCapabilityRepository.findByQuery(null, offset)).thenReturn(page);
      when(roleCapabilityEntityMapper.convert(entity)).thenReturn(expectedRoleCapability);

      var result = roleCapabilityService.find(null, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedRoleCapability));
    }

    @Test
    void positive_withQuery() {
      var query = "cql.allRecords=1";
      var offset = OffsetRequest.of(0, 10, DEFAULT_ROLE_CAPABILITY_SORT);
      var entity = roleCapabilityEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedRoleCapability = roleCapability();

      when(roleCapabilityRepository.findByQuery(query, offset)).thenReturn(page);
      when(roleCapabilityEntityMapper.convert(entity)).thenReturn(expectedRoleCapability);

      var result = roleCapabilityService.find(query, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedRoleCapability));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive_plainCapabilities() {
      var roleCapability1 = roleCapability(capabilityId1);
      var roleCapability2 = roleCapability(capabilityId2);
      var capabilityIds = List.of(capabilityId1, capabilityId2);
      var roleCapabilityEntity1 = roleCapabilityEntity(capabilityId1);
      var roleCapabilityEntity2 = roleCapabilityEntity(capabilityId2);
      var entities = List.of(roleCapabilityEntity1, roleCapabilityEntity2);
      var endpoints = List.of(endpoint("/c1", GET), endpoint("/c2", GET));

      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);
      when(roleCapabilityRepository.saveAll(entities)).thenReturn(entities);
      when(roleCapabilityRepository.findRoleCapabilities(ROLE_ID, capabilityIds)).thenReturn(emptyList());
      when(roleCapabilityEntityMapper.convert(roleCapabilityEntity1)).thenReturn(roleCapability1);
      when(roleCapabilityEntityMapper.convert(roleCapabilityEntity2)).thenReturn(roleCapability2);
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(capabilityEndpointService.getByCapabilityIds(capabilityIds, emptyList())).thenReturn(endpoints);

      var result = roleCapabilityService.create(ROLE_ID, capabilityIds, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability1, roleCapability2));
      verify(capabilityService).checkIds(capabilityIds);
    }

    @Test
    void positive_plainCapabilitiesByNames() {
      var roleCapability = roleCapability(capabilityId1);
      var capabilities = List.of(capability(capabilityId1));
      var capabilityIds = List.of(capabilityId1);
      var capabilityNames = List.of(CAPABILITY_NAME);
      var roleCapabilityEntity1 = roleCapabilityEntity(capabilityId1);
      var entities = List.of(roleCapabilityEntity1);
      var request = new RoleCapabilitiesRequest().roleId(ROLE_ID).addCapabilityNamesItem(CAPABILITY_NAME);
      var endpoints = List.of(endpoint("/c1", GET), endpoint("/c2", GET));

      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);
      when(roleCapabilityRepository.saveAll(entities)).thenReturn(entities);
      when(roleCapabilityRepository.findRoleCapabilities(ROLE_ID, capabilityIds)).thenReturn(emptyList());
      when(roleCapabilityEntityMapper.convert(roleCapabilityEntity1)).thenReturn(roleCapability);
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(capabilityService.findByNames(capabilityNames)).thenReturn(capabilities);
      when(capabilityEndpointService.getByCapabilityIds(capabilityIds, emptyList())).thenReturn(endpoints);

      var result = roleCapabilityService.create(request, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability));
      verify(capabilityService).checkIds(capabilityIds);
    }

    @Test
    void positive_capabilitySetAssigned() {
      var roleCapability1 = roleCapability(capabilityId1);
      var roleCapability2 = roleCapability(capabilityId2);
      var capabilityIds = List.of(capabilityId1, capabilityId2);
      var roleCapabilityEntity1 = roleCapabilityEntity(capabilityId1);
      var roleCapabilityEntity2 = roleCapabilityEntity(capabilityId2);
      var entities = List.of(roleCapabilityEntity1, roleCapabilityEntity2);
      var endpoints = List.of(endpoint("/c1", GET));
      var assignedCapabilityIds = List.of(capabilityId2, capabilityId3);
      var capabilitySet = capabilitySet(assignedCapabilityIds);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);
      when(roleCapabilityRepository.saveAll(entities)).thenReturn(entities);
      when(roleCapabilityRepository.findRoleCapabilities(ROLE_ID, capabilityIds)).thenReturn(emptyList());
      when(roleCapabilityEntityMapper.convert(roleCapabilityEntity1)).thenReturn(roleCapability1);
      when(roleCapabilityEntityMapper.convert(roleCapabilityEntity2)).thenReturn(roleCapability2);
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(asSinglePage(capabilitySet));
      when(capabilityEndpointService.getByCapabilityIds(capabilityIds, assignedCapabilityIds)).thenReturn(endpoints);

      var result = roleCapabilityService.create(ROLE_ID, capabilityIds, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability1, roleCapability2));
      verify(capabilityService).checkIds(capabilityIds);
    }

    @Test
    void negative_emptyCapabilities() {
      var capabilityIds = Collections.<UUID>emptyList();
      assertThatThrownBy(() -> roleCapabilityService.create(ROLE_ID, capabilityIds, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Capability id list is empty");
    }

    @Test
    void positive_existingAssignmentWithCreateSafe() {
      var capIds = List.of(capabilityId1);
      var roleCapabilityEntity = roleCapabilityEntity(ROLE_ID, capabilityId1);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findRoleCapabilities(ROLE_ID, capIds)).thenReturn(List.of(roleCapabilityEntity));

      var result = roleCapabilityService.create(ROLE_ID, capIds, true);

      assertThat(result).isEqualTo(PageResult.empty());
    }

    @Test
    void negative_existingAssignment() {
      var capIds = List.of(capabilityId1, capabilityId2);
      var roleCapabilityEntity = roleCapabilityEntity(ROLE_ID, capabilityId1);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findRoleCapabilities(ROLE_ID, capIds)).thenReturn(List.of(roleCapabilityEntity));

      assertThatThrownBy(() -> roleCapabilityService.create(ROLE_ID, capIds, false))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Relation already exists for role='%s' and capabilities=[%s]", ROLE_ID, capabilityId1);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role is not found by id: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      var capabilityIds = List.of(capabilityId1);
      assertThatThrownBy(() -> roleCapabilityService.create(ROLE_ID, capabilityIds, false))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }
  }

  @Nested
  @DisplayName("deleteAll")
  class DeleteAll {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive_plainCapabilities() {
      var expectedEntities = List.of(roleCapabilityEntity(ROLE_ID, capabilityId1));
      var endpoints = List.of(endpoint("/c1", GET));
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(expectedEntities);
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(capabilityEndpointService.getByCapabilityIds(List.of(capabilityId1), emptyList())).thenReturn(endpoints);

      roleCapabilityService.deleteAll(ROLE_ID);

      verify(roleCapabilityRepository).deleteRoleCapabilities(ROLE_ID, List.of(capabilityId1));
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
    }

    @Test
    void positive_capabilitySetAssigned() {
      var expectedEntities = List.of(roleCapabilityEntity(capabilityId1), roleCapabilityEntity(capabilityId2));
      var endpoints = List.of(endpoint("/c2", GET));
      var assignedIds = List.of(capabilityId1, capabilityId3);
      var deprecatedIds = List.of(capabilityId1, capabilityId2);
      var capabilitySet = capabilitySet(assignedIds);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(expectedEntities);
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(asSinglePage(capabilitySet));
      when(capabilityEndpointService.getByCapabilityIds(deprecatedIds, assignedIds)).thenReturn(endpoints);

      roleCapabilityService.deleteAll(ROLE_ID);

      verify(roleCapabilityRepository).deleteRoleCapabilities(ROLE_ID, deprecatedIds);
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role capabilities are not found for role: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      assertThatThrownBy(() -> roleCapabilityService.deleteAll(ROLE_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_roleCapabilitiesAreNotFound() {
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(emptyList());
      assertThatThrownBy(() -> roleCapabilityService.deleteAll(ROLE_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Relations between role and capabilities are not found for role: %s", ROLE_ID);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    void positive() {
      var existingEntity = roleCapabilityEntity();
      var existingEntities = List.of(existingEntity);
      var capabilitySetIds = List.of(CAPABILITY_ID);
      var endpoints = List.of(endpoint());
      var entityKey = RoleCapabilityKey.of(ROLE_ID, CAPABILITY_ID);

      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilityRepository.findById(entityKey)).thenReturn(Optional.of(existingEntity));
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(empty());
      when(capabilityEndpointService.getByCapabilityIds(capabilitySetIds, emptyList())).thenReturn(endpoints);

      roleCapabilityService.delete(ROLE_ID, CAPABILITY_ID);

      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
      verify(roleCapabilityRepository).deleteRoleCapabilities(ROLE_ID, capabilitySetIds);
    }

    @Test
    void positive_entityNotFound() {
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(emptyList());
      roleCapabilityService.delete(ROLE_ID, CAPABILITY_ID);
      verifyNoInteractions(rolePermissionService);
      verify(roleCapabilityRepository, never()).deleteRoleCapabilities(any(), anyList());
    }

    @Test
    void positive_entityNotFoundById() {
      var existingEntity = roleCapabilityEntity();
      var existingEntities = List.of(existingEntity);
      var entityKey = RoleCapabilityKey.of(ROLE_ID, CAPABILITY_ID);

      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilityRepository.findById(entityKey)).thenReturn(Optional.empty());

      roleCapabilityService.delete(ROLE_ID, CAPABILITY_ID);

      verifyNoInteractions(rolePermissionService);
      verify(roleCapabilityRepository, never()).deleteRoleCapabilities(any(), anyList());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive() {
      var uce1 = roleCapabilityEntity(capabilityId1);
      var uce2 = roleCapabilityEntity(capabilityId2);
      var uce3 = roleCapabilityEntity(capabilityId3);
      var existingEntities = List.of(uce1, uce3);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilityEntityMapper.convert(uce2)).thenReturn(roleCapability(ROLE_ID, capabilityId2));
      when(roleCapabilityRepository.saveAll(List.of(uce2))).thenReturn(List.of(uce2));

      var newIds = List.of(capabilityId2);
      var deprecatedIds = List.of(capabilityId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDelete = List.of(endpoint("/c1", GET));
      when(capabilityEndpointService.getByCapabilityIds(eq(newIds), anyList())).thenReturn(endpointsToAssign);
      when(capabilityEndpointService.getByCapabilityIds(eq(deprecatedIds), anyList())).thenReturn(endpointsToDelete);

      var requestCapabilityIds = List.of(capabilityId2, capabilityId3);
      roleCapabilityService.update(ROLE_ID, requestCapabilityIds);

      verify(capabilityService).checkIds(List.of(capabilityId2));
      verify(rolePermissionService).createPermissions(ROLE_ID, endpointsToAssign);
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpointsToDelete);
      verify(roleCapabilityRepository).deleteRoleCapabilities(ROLE_ID, deprecatedIds);
      verify(capabilityEndpointService).getByCapabilityIds(newIds, List.of(capabilityId1, capabilityId3));
      verify(capabilityEndpointService).getByCapabilityIds(deprecatedIds, List.of(capabilityId3, capabilityId2));
    }

    @Test
    void negative_notingToUpdate() {
      var roleCapabilityEntity = roleCapabilityEntity(ROLE_ID, capabilityId1);
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilityRepository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(roleCapabilityEntity));

      var capabilityIds = List.of(capabilityId1);
      roleCapabilityService.update(ROLE_ID, capabilityIds);
      verifyNoInteractions(rolePermissionService);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role is not found by id: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));

      var capabilityIds = List.of(capabilityId1);
      assertThatThrownBy(() -> roleCapabilityService.update(ROLE_ID, capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Role is not found by id: %s", ROLE_ID);
    }
  }

  @Nested
  @DisplayName("getCapabilitySetCapabilityIds")
  class GetCapabilitySetCapabilityIds {

    @Test
    void positive() {
      var capabilityId = UUID.randomUUID();
      var assignedIds = List.of(capabilityId);
      var capabilitySet = capabilitySet(assignedIds);
      when(capabilitySetService.findByRoleId(ROLE_ID, MAX_VALUE, 0)).thenReturn(asSinglePage(capabilitySet));

      var result = roleCapabilityService.getCapabilitySetCapabilityIds(ROLE_ID);

      assertThat(result).containsExactly(capabilityId);
    }
  }
}
