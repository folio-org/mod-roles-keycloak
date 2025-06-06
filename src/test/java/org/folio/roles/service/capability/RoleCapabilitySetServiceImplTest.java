package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.entity.RoleCapabilitySetEntity.DEFAULT_ROLE_CAPABILITY_SET_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.domain.model.PageResult.empty;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_NAME_2;
import static org.folio.roles.support.CapabilitySetUtils.INVALID_CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySet;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.mapper.entity.RoleCapabilitySetEntityMapper;
import org.folio.roles.repository.RoleCapabilitySetRepository;
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
class RoleCapabilitySetServiceImplTest {

  @InjectMocks private RoleCapabilitySetServiceImpl roleCapabilitySetService;

  @Mock private RoleService roleService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityEndpointService endpointService;
  @Mock private RolePermissionService rolePermissionService;
  @Mock private RoleCapabilitySetRepository roleCapabilitySetRepository;
  @Mock private RoleCapabilitySetEntityMapper roleCapabilitySetEntityMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_emptyQuery() {
      var offset = OffsetRequest.of(0, 10, DEFAULT_ROLE_CAPABILITY_SET_SORT);
      var entity = roleCapabilitySetEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedRoleCapability = roleCapabilitySet();

      when(roleCapabilitySetRepository.findByQuery(null, offset)).thenReturn(page);
      when(roleCapabilitySetEntityMapper.convert(entity)).thenReturn(expectedRoleCapability);

      var result = roleCapabilitySetService.find(null, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedRoleCapability));
    }

    @Test
    void positive_withQuery() {
      var query = "cql.allRecords=1";
      var offset = OffsetRequest.of(0, 10, DEFAULT_ROLE_CAPABILITY_SET_SORT);
      var entity = roleCapabilitySetEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedRoleCapability = roleCapabilitySet();

      when(roleCapabilitySetRepository.findByQuery(query, offset)).thenReturn(page);
      when(roleCapabilitySetEntityMapper.convert(entity)).thenReturn(expectedRoleCapability);

      var result = roleCapabilitySetService.find(query, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedRoleCapability));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    private final UUID capabilitySetId1 = UUID.randomUUID();
    private final UUID capabilitySetId2 = UUID.randomUUID();

    @Test
    void positive() {
      var roleCapability1 = roleCapabilitySet(capabilitySetId1);
      var roleCapability2 = roleCapabilitySet(capabilitySetId2);
      var capabilitySetIds = List.of(capabilitySetId1, capabilitySetId2);
      var roleCapabilityEntity1 = roleCapabilitySetEntity(capabilitySetId1);
      var roleCapabilityEntity2 = roleCapabilitySetEntity(capabilitySetId2);
      var entities = List.of(roleCapabilityEntity1, roleCapabilityEntity2);
      var endpoints = List.of(endpoint());

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetEntityMapper.convert(roleCapabilityEntity1)).thenReturn(roleCapability1);
      when(roleCapabilitySetEntityMapper.convert(roleCapabilityEntity2)).thenReturn(roleCapability2);
      when(roleCapabilitySetRepository.findRoleCapabilitySets(ROLE_ID, capabilitySetIds)).thenReturn(emptyList());
      when(roleCapabilitySetRepository.saveAll(entities)).thenReturn(entities);
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);
      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);

      var result = roleCapabilitySetService.create(ROLE_ID, capabilitySetIds, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability1, roleCapability2));
      verify(capabilitySetService).checkIds(capabilitySetIds);
    }

    @Test
    void positive_capabilityNamesInRequest() {
      var roleCapability = roleCapabilitySet(capabilitySetId1);
      var capabilitySet = capabilitySet(capabilitySetId1).name(CAPABILITY_SET_NAME);
      var capabilitySets = List.of(capabilitySet);
      var capabilitySetIds = List.of(capabilitySetId1);
      var capabilitySetNames = List.of(CAPABILITY_SET_NAME);
      var roleCapabilityEntity = roleCapabilitySetEntity(capabilitySetId1);
      var entities = List.of(roleCapabilityEntity);
      var request = new RoleCapabilitySetsRequest().roleId(ROLE_ID).addCapabilitySetNamesItem(CAPABILITY_SET_NAME);
      var endpoints = List.of(endpoint());

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetEntityMapper.convert(roleCapabilityEntity)).thenReturn(roleCapability);
      when(roleCapabilitySetRepository.findRoleCapabilitySets(ROLE_ID, capabilitySetIds)).thenReturn(emptyList());
      when(roleCapabilitySetRepository.saveAll(entities)).thenReturn(entities);
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(capabilitySets);
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);
      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);

      var result = roleCapabilitySetService.create(request, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability));
      verify(capabilitySetService).checkIds(capabilitySetIds);
    }

    @Test
    void positive_capabilityNamesInRequest_skipInvalidNames() {
      var roleCapability = roleCapabilitySet(capabilitySetId1);
      var capabilitySet = capabilitySet(capabilitySetId1).name(CAPABILITY_SET_NAME);
      var capabilitySets = List.of(capabilitySet);
      var capabilitySetIds = List.of(capabilitySetId1);
      var capabilitySetNames = List.of(CAPABILITY_SET_NAME, INVALID_CAPABILITY_SET_NAME);
      var roleCapabilityEntity = roleCapabilitySetEntity(capabilitySetId1);
      var entities = List.of(roleCapabilityEntity);
      var request = new RoleCapabilitySetsRequest().roleId(ROLE_ID)
        .addCapabilitySetNamesItem(CAPABILITY_SET_NAME)
        .addCapabilitySetNamesItem(INVALID_CAPABILITY_SET_NAME);
      var endpoints = List.of(endpoint());

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetEntityMapper.convert(roleCapabilityEntity)).thenReturn(roleCapability);
      when(roleCapabilitySetRepository.findRoleCapabilitySets(ROLE_ID, capabilitySetIds)).thenReturn(emptyList());
      when(roleCapabilitySetRepository.saveAll(entities)).thenReturn(entities);
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(capabilitySets);
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);
      doNothing().when(rolePermissionService).createPermissions(ROLE_ID, endpoints);

      var result = roleCapabilitySetService.create(request, false);

      assertThat(result).isEqualTo(asSinglePage(roleCapability));
      verify(capabilitySetService).checkIds(capabilitySetIds);
      verify(rolePermissionService).createPermissions(ROLE_ID, endpoints);
      verify(endpointService).getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList());
    }

    @Test
    void negative_emptyCapabilities() {
      var capabilityIds = Collections.<UUID>emptyList();
      assertThatThrownBy(() -> roleCapabilitySetService.create(ROLE_ID, capabilityIds, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List with capability set identifiers is empty");
    }

    @Test
    void positive_existingAssignmentWithCreateSafe() {
      var capIds = List.of(capabilitySetId1);
      var roleCapabilityEntity = roleCapabilitySetEntity(ROLE_ID, capabilitySetId1);
      var foundEntities = List.of(roleCapabilityEntity);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findRoleCapabilitySets(ROLE_ID, capIds)).thenReturn(foundEntities);

      var result = roleCapabilitySetService.create(ROLE_ID, capIds, true);

      assertThat(result).isEqualTo(PageResult.empty());
    }

    @Test
    void negative_existingAssignment() {
      var capIds = List.of(capabilitySetId1, capabilitySetId2);
      var roleCapabilityEntity = roleCapabilitySetEntity(ROLE_ID, capabilitySetId1);
      var foundEntities = List.of(roleCapabilityEntity);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findRoleCapabilitySets(ROLE_ID, capIds)).thenReturn(foundEntities);

      assertThatThrownBy(() -> roleCapabilitySetService.create(ROLE_ID, capIds, false))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Relation already exists for role='%s' and capabilitySets=[%s]", ROLE_ID, capabilitySetId1);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role is not found by id: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      assertThatThrownBy(() -> roleCapabilitySetService.create(ROLE_ID, capabilityIds, false))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_capabilitySetNameIsNotFound() {
      var errorMessage = "Capability sets by name are not found";
      var capabilitySetNames = List.of(INVALID_CAPABILITY_SET_NAME);
      var request = new RoleCapabilitySetsRequest()
        .roleId(ROLE_ID)
        .capabilitySetNames(capabilitySetNames);

      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(List.of());

      assertThatThrownBy(() -> roleCapabilitySetService.create(request, false))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage(errorMessage);
    }
  }

  @Nested
  @DisplayName("deleteAll")
  class DeleteAll {

    @Test
    void positive() {
      var expectedEntities = List.of(roleCapabilitySetEntity());
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);
      var endpoints = List.of(endpoint());

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(expectedEntities);
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);

      roleCapabilitySetService.deleteAll(ROLE_ID);

      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, capabilitySetIds);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role is not found by id: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      assertThatThrownBy(() -> roleCapabilitySetService.deleteAll(ROLE_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_roleCapabilitiesAreNotFound() {
      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(emptyList());
      assertThatThrownBy(() -> roleCapabilitySetService.deleteAll(ROLE_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Relations between role and capability sets are not found for role: %s", ROLE_ID);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    void positive() {
      var existingEntity = roleCapabilitySetEntity();
      var existingEntities = List.of(existingEntity);
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);
      var endpoints = List.of(endpoint());
      var entityKey = RoleCapabilitySetKey.of(ROLE_ID, CAPABILITY_SET_ID);

      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilitySetRepository.findById(entityKey)).thenReturn(Optional.of(existingEntity));
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);

      roleCapabilitySetService.delete(ROLE_ID, CAPABILITY_SET_ID);

      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, capabilitySetIds);
    }

    @Test
    void positive_collection() {
      var existingEntity = roleCapabilitySetEntity();
      var existingEntities = List.of(existingEntity);
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);
      var endpoints = List.of(endpoint());

      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);

      roleCapabilitySetService.delete(ROLE_ID, List.of(CAPABILITY_SET_ID));

      verify(rolePermissionService).deletePermissions(ROLE_ID, endpoints);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, capabilitySetIds);
    }

    @Test
    void positive_collectionAndNoCapabilitySetsFound() {
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(emptyList());

      roleCapabilitySetService.delete(ROLE_ID, List.of(CAPABILITY_SET_ID));

      verify(rolePermissionService, never()).deletePermissions(any(), anyList());
      verify(roleCapabilitySetRepository, never()).deleteRoleCapabilitySets(any(), anyList());
    }

    @Test
    void positive_emptyCollection() {
      roleCapabilitySetService.delete(ROLE_ID, emptyList());

      verify(rolePermissionService, never()).deletePermissions(any(), anyList());
      verify(roleCapabilitySetRepository, never()).deleteRoleCapabilitySets(any(), anyList());
    }

    @Test
    void positive_entityNotFound() {
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(emptyList());
      roleCapabilitySetService.delete(ROLE_ID, CAPABILITY_SET_ID);
      verifyNoInteractions(rolePermissionService);
      verify(roleCapabilitySetRepository, never()).deleteRoleCapabilitySets(any(), anyList());
    }

    @Test
    void positive_entityNotFoundById() {
      var existingEntity = roleCapabilitySetEntity();
      var existingEntities = List.of(existingEntity);
      var entityKey = RoleCapabilitySetKey.of(ROLE_ID, CAPABILITY_SET_ID);

      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilitySetRepository.findById(entityKey)).thenReturn(Optional.empty());

      roleCapabilitySetService.delete(ROLE_ID, CAPABILITY_SET_ID);

      verifyNoInteractions(rolePermissionService);
      verify(roleCapabilitySetRepository, never()).deleteRoleCapabilitySets(any(), anyList());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UUID capabilitySetId1 = UUID.randomUUID();
    private final UUID capabilitySetId2 = UUID.randomUUID();
    private final UUID capabilitySetId3 = UUID.randomUUID();

    @Test
    void positive() {
      var ucse1 = roleCapabilitySetEntity(capabilitySetId1);
      var ucse2 = roleCapabilitySetEntity(capabilitySetId2);
      var ucse3 = roleCapabilitySetEntity(capabilitySetId3);
      var existingEntities = List.of(ucse1, ucse3);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilitySetEntityMapper.convert(ucse2)).thenReturn(roleCapabilitySet(ROLE_ID, capabilitySetId2));
      when(roleCapabilitySetRepository.saveAll(List.of(ucse2))).thenReturn(List.of(ucse2));
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(PageResult.empty());

      var newIds = List.of(capabilitySetId2);
      var deprecatedIds = List.of(capabilitySetId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDel = List.of(endpoint("/c1", GET));
      var idsToAssign = List.of(capabilitySetId1, capabilitySetId3);
      var assignedIds = Set.of(capabilitySetId2, capabilitySetId3);

      when(endpointService.getByCapabilitySetIds(newIds, idsToAssign, emptyList())).thenReturn(endpointsToAssign);
      when(endpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, emptyList())).thenReturn(endpointsToDel);

      var requestCapabilityIds = List.of(capabilitySetId2, capabilitySetId3);
      roleCapabilitySetService.update(ROLE_ID, requestCapabilityIds);

      verify(capabilitySetService).checkIds(newIds);
      verify(rolePermissionService).createPermissions(ROLE_ID, endpointsToAssign);
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpointsToDel);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, deprecatedIds);
      verify(capabilityService, times(2)).findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0);
    }

    @Test
    void positiveByName() {
      var ucse1 = roleCapabilitySetEntity(capabilitySetId1);
      var ucse2 = roleCapabilitySetEntity(capabilitySetId2);
      var ucse3 = roleCapabilitySetEntity(capabilitySetId3);
      var existingEntities = List.of(ucse1, ucse3);

      var capabilitySet2 = capabilitySet().id(capabilitySetId2).name(CAPABILITY_SET_NAME);
      var capabilitySet3 = capabilitySet().id(capabilitySetId3).name(CAPABILITY_SET_NAME_2);
      var capabilitySets = List.of(capabilitySet2, capabilitySet3);
      var capabilitySetNames = List.of(capabilitySet2.getName(), capabilitySet3.getName());

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(capabilitySets);
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilitySetEntityMapper.convert(ucse2)).thenReturn(roleCapabilitySet(ROLE_ID, capabilitySetId2));
      when(roleCapabilitySetRepository.saveAll(List.of(ucse2))).thenReturn(List.of(ucse2));
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(PageResult.empty());

      var newIds = List.of(capabilitySetId2);
      var deprecatedIds = List.of(capabilitySetId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDel = List.of(endpoint("/c1", GET));
      var idsToAssign = List.of(capabilitySetId1, capabilitySetId3);
      var assignedIds = Set.of(capabilitySetId2, capabilitySetId3);

      when(endpointService.getByCapabilitySetIds(newIds, idsToAssign, emptyList())).thenReturn(endpointsToAssign);
      when(endpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, emptyList())).thenReturn(endpointsToDel);

      var request = new CapabilitySetsUpdateRequest().capabilitySetNames(capabilitySetNames);
      roleCapabilitySetService.update(ROLE_ID, request);

      verify(capabilitySetService).checkIds(newIds);
      verify(rolePermissionService).createPermissions(ROLE_ID, endpointsToAssign);
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpointsToDel);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, deprecatedIds);
      verify(capabilityService, times(2)).findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0);
    }

    @Test
    void positiveByName_SkipInvalidName() {
      var ucse1 = roleCapabilitySetEntity(capabilitySetId1);
      var ucse2 = roleCapabilitySetEntity(capabilitySetId2);
      var ucse3 = roleCapabilitySetEntity(capabilitySetId3);
      var existingEntities = List.of(ucse1, ucse3);

      var capabilitySet2 = capabilitySet().id(capabilitySetId2).name(CAPABILITY_SET_NAME);
      var capabilitySet3 = capabilitySet().id(capabilitySetId3).name(CAPABILITY_SET_NAME_2);
      var capabilitySets = List.of(capabilitySet2, capabilitySet3);
      var capabilitySetNames = List.of(capabilitySet2.getName(), capabilitySet3.getName(), INVALID_CAPABILITY_SET_NAME);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(capabilitySets);
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(existingEntities);
      when(roleCapabilitySetEntityMapper.convert(ucse2)).thenReturn(roleCapabilitySet(ROLE_ID, capabilitySetId2));
      when(roleCapabilitySetRepository.saveAll(List.of(ucse2))).thenReturn(List.of(ucse2));
      when(capabilityService.findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0)).thenReturn(PageResult.empty());

      var newIds = List.of(capabilitySetId2);
      var deprecatedIds = List.of(capabilitySetId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDel = List.of(endpoint("/c1", GET));
      var idsToAssign = List.of(capabilitySetId1, capabilitySetId3);
      var assignedIds = Set.of(capabilitySetId2, capabilitySetId3);

      when(endpointService.getByCapabilitySetIds(newIds, idsToAssign, emptyList())).thenReturn(endpointsToAssign);
      when(endpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, emptyList())).thenReturn(endpointsToDel);

      var request = new CapabilitySetsUpdateRequest().capabilitySetNames(capabilitySetNames);
      roleCapabilitySetService.update(ROLE_ID, request);

      verify(capabilitySetService).checkIds(newIds);
      verify(rolePermissionService).createPermissions(ROLE_ID, endpointsToAssign);
      verify(rolePermissionService).deletePermissions(ROLE_ID, endpointsToDel);
      verify(roleCapabilitySetRepository).deleteRoleCapabilitySets(ROLE_ID, deprecatedIds);
      verify(capabilityService, times(2)).findByRoleId(ROLE_ID, false, false, MAX_VALUE, 0);
      verify(endpointService).getByCapabilitySetIds(newIds, idsToAssign, emptyList());
      verify(endpointService).getByCapabilitySetIds(deprecatedIds, assignedIds, emptyList());
    }

    @Test
    void negative_notingToUpdate() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(roleCapabilitySetRepository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(roleCapabilitySetEntity()));

      roleCapabilitySetService.update(ROLE_ID, capabilityIds);

      verifyNoInteractions(rolePermissionService);
    }

    @Test
    void negative_roleIsNotFound() {
      var errorMessage = "Role is not found by id: " + ROLE_ID;
      when(roleService.getById(ROLE_ID)).thenThrow(new EntityNotFoundException(errorMessage));

      var capabilitySetIds = List.of(capabilitySetId1);
      assertThatThrownBy(() -> roleCapabilitySetService.update(ROLE_ID, capabilitySetIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_capabilitySetNameIsNotFound() {
      var errorMessage = "Capability sets by name are not found";
      var capabilitySetNames = List.of(INVALID_CAPABILITY_SET_NAME);
      var request = new CapabilitySetsUpdateRequest().capabilitySetNames(capabilitySetNames);

      when(capabilitySetService.findByNames(capabilitySetNames)).thenReturn(List.of());

      assertThatThrownBy(() -> roleCapabilitySetService.update(ROLE_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage(errorMessage);
    }
  }
}
