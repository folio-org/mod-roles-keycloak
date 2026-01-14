package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.entity.CapabilityEntity.DEFAULT_CAPABILITY_SORT;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.PERMISSION_NAME;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.CapabilityUtils.technicalCapability;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.support.TestUtils;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CapabilityServiceTest {

  @InjectMocks private CapabilityService capabilityService;
  @Mock private CapabilityRepository capabilityRepository;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityEntityMapper capabilityEntityMapper;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private UserPermissionCacheService userPermissionCacheService;
  @Captor private ArgumentCaptor<CapabilityEvent> eventCaptor;

  @BeforeEach
  void setUp() {
    var folioExecutionContext = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    this.capabilityService = new CapabilityService(capabilityRepository, folioExecutionContext,
      capabilityEntityMapper, applicationEventPublisher, userPermissionCacheService, capabilitySetService);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("update")
  class Update {

    private void verifyCapturedEvents(CapabilityEvent... expectedEvents) {
      assertThat(eventCaptor.getAllValues())
        .usingRecursiveComparison()
        .ignoringFields("timestamp", "context")
        .isEqualTo(List.of(expectedEvents));
    }

    @Test
    void positive() {
      var capability = capability().id(null);
      var savedCapability = capability();
      var capabilityEntity = capabilityEntity(null);
      var savedCapabilityEntity = capabilityEntity();
      when(capabilityRepository.findAllByNamesIncludeDummy(Set.of("test_resource.create"))).thenReturn(emptyList());
      when(capabilityEntityMapper.convert(capability)).thenReturn(capabilityEntity);
      when(capabilityRepository.saveAll(List.of(capabilityEntity))).thenReturn(List.of(savedCapabilityEntity));
      when(capabilityEntityMapper.convert(savedCapabilityEntity)).thenReturn(savedCapability);
      doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

      capabilityService.update(ResourceEventType.CREATE, List.of(capability), emptyList());

      verify(capabilityRepository).saveAll(List.of(capabilityEntity));
      verifyCapturedEvents(CapabilityEvent.created(savedCapability));
    }

    @Test
    void positive_capabilityByNameExists(CapturedOutput output) {
      var existingCapability = capability();
      var existingEntity = capabilityEntity();
      when(capabilityRepository.findAllByNamesIncludeDummy(Set.of("test_resource.create")))
        .thenReturn(List.of(existingEntity));
      when(capabilityEntityMapper.convert(existingEntity)).thenReturn(existingCapability);

      var updatedCapabilityEntity = capabilityEntity();
      updatedCapabilityEntity.setDescription("updated name");
      when(capabilityRepository.saveAll(List.of(updatedCapabilityEntity))).thenReturn(List.of(updatedCapabilityEntity));

      var updatedCapability = capability().description("updated name");
      when(capabilityEntityMapper.convert(updatedCapability)).thenReturn(updatedCapabilityEntity);
      when(capabilityEntityMapper.convert(updatedCapabilityEntity)).thenReturn(updatedCapability);
      doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

      var newCapability = capability().id(null).description("updated name");
      capabilityService.update(ResourceEventType.CREATE, List.of(newCapability), emptyList());

      verify(capabilityRepository).saveAll(List.of(updatedCapabilityEntity));
      verifyCapturedEvents(CapabilityEvent.updated(updatedCapability, existingCapability));
      assertThat(output.getAll()).contains("Duplicated capabilities has been updated: [test_resource.create]");
    }

    @Test
    void positive_capabilityByNameExistsForUpgrade() {
      var existingCapability = capability();
      var existingEntity = capabilityEntity();
      when(capabilityRepository.findAllByNamesIncludeDummy(Set.of("test_resource.create")))
        .thenReturn(List.of(existingEntity));
      when(capabilityEntityMapper.convert(existingEntity)).thenReturn(existingCapability);

      var updatedCapabilityEntity = capabilityEntity();
      updatedCapabilityEntity.setDescription("updated name");
      when(capabilityRepository.saveAll(List.of(updatedCapabilityEntity))).thenReturn(List.of(updatedCapabilityEntity));

      var updatedCapability = capability().description("updated name");
      when(capabilityEntityMapper.convert(updatedCapability)).thenReturn(updatedCapabilityEntity);
      when(capabilityEntityMapper.convert(updatedCapabilityEntity)).thenReturn(updatedCapability);
      doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

      var newCapability = capability().id(null).description("updated name");
      capabilityService.update(ResourceEventType.UPDATE, List.of(newCapability), emptyList());

      verify(capabilityRepository).saveAll(List.of(updatedCapabilityEntity));
      verifyCapturedEvents(CapabilityEvent.updated(updatedCapability, existingCapability));
    }

    @Test
    void positive_deprecatedCapability() {
      var capability = capability().id(null);
      var existingCapability = capability();
      var existingCapabilityEntity = capabilityEntity();

      var capabilityNames = Set.of("test_resource.create");

      when(capabilityRepository.findAllByNames(capabilityNames)).thenReturn(List.of(existingCapabilityEntity));
      when(capabilityEntityMapper.convert(existingCapabilityEntity)).thenReturn(existingCapability);
      doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

      capabilityService.update(ResourceEventType.UPDATE, emptyList(), List.of(capability));

      verifyCapturedEvents(CapabilityEvent.deleted(existingCapability));
    }

    @Test
    void positive_deprecatedCapabilityNotFoundByNames() {
      var capability = capability().id(null);
      var capabilityNames = Set.of("test_resource.create");
      when(capabilityRepository.findAllByNames(capabilityNames)).thenReturn(emptyList());

      capabilityService.update(ResourceEventType.UPDATE, emptyList(), List.of(capability));

      verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void positive_emptyNewAndOldCapabilities() {
      capabilityService.update(CREATE, emptyList(), emptyList());
      Mockito.verifyNoInteractions(capabilityRepository, capabilityEntityMapper);
    }

    @Test
    void positive_updateDummyCapabilities() {
      var capability = capability().id(null);
      var capabilityEntity = capabilityEntity(null);
      var dummyCapability = capability().id(null);
      dummyCapability.setDummyCapability(true);
      dummyCapability.setEndpoints(null);
      var dummyCapabilityEntity = capabilityEntity();
      dummyCapabilityEntity.setDummyCapability(true);
      var savedCapabilityForDummy = capability();
      savedCapabilityForDummy.setDummyCapability(false);
      var savedCapabilityEntity = capabilityEntity();

      when(capabilityRepository.findAllByNamesIncludeDummy(Set.of("test_resource.create")))
        .thenReturn(List.of(dummyCapabilityEntity));
      when(capabilityEntityMapper.convert(capability)).thenReturn(capabilityEntity);
      when(capabilityRepository.saveAll(List.of(capabilityEntity))).thenReturn(List.of(savedCapabilityEntity));
      when(capabilityEntityMapper.convert(savedCapabilityEntity)).thenReturn(savedCapabilityForDummy);
      when(capabilityEntityMapper.convert(dummyCapabilityEntity)).thenReturn(dummyCapability);
      doNothing().when(applicationEventPublisher).publishEvent(eventCaptor.capture());

      capabilityService.update(ResourceEventType.CREATE, List.of(capability), emptyList());

      verify(capabilityRepository).saveAll(List.of(capabilityEntity));
      verifyCapturedEvents(CapabilityEvent.updated(savedCapabilityForDummy, dummyCapability));
    }
  }

  @Nested
  @DisplayName("get")
  class Get {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      when(capabilityRepository.getReferenceById(CAPABILITY_ID)).thenReturn(capabilityEntity);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.get(CAPABILITY_ID);

      assertThat(result).isEqualTo(capability);
    }

    @Test
    void negative_entityNotFound() {
      when(capabilityRepository.getReferenceById(CAPABILITY_ID)).thenThrow(EntityNotFoundException.class);
      assertThatThrownBy(() -> capabilityService.get(CAPABILITY_ID))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive() {
      var query = "cql.allRecords = 1";
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByQuery(query, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.find(query, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }

    @Test
    void positive_noQuery() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByQuery(null, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.find(null, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive_expandIsFalseAndIncludeDummyIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByUserId(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, false, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByUserId(USER_ID, offsetRequest);
    }

    @Test
    void positive_expandIsFalseAndIncludeDummyIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByUserIdIncludeDummy(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, false, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByUserIdIncludeDummy(USER_ID, offsetRequest);
    }

    @Test
    void positive_expandIsTrueAndIncludeDummyIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByUserId(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, true, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findAllByUserId(USER_ID, offsetRequest);
    }

    @Test
    void positive_expandIsTrueAndIncludeDummyIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByUserIdIncludeDummy(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, true, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findAllByUserIdIncludeDummy(USER_ID, offsetRequest);
    }
  }

  @Nested
  @DisplayName("findByRoleId")
  class FindByRoleId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive_expandIsFalseAndIncludeDummyIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByRoleId(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, false, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByRoleId(ROLE_ID, offsetRequest);
    }

    @Test
    void positive_expandIsFalseAndIncludeDummyIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByRoleIdIncludeDummy(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, false, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByRoleIdIncludeDummy(ROLE_ID, offsetRequest);
    }

    @Test
    void positive_expandIsTrueAndIncludeDummyIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByRoleId(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, true, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findAllByRoleId(ROLE_ID, offsetRequest);
    }

    @Test
    void positive_expandIsTrueAndIncludeDummyIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByRoleIdIncludeDummy(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, true, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findAllByRoleIdIncludeDummy(ROLE_ID, offsetRequest);
    }
  }

  @Nested
  @DisplayName("findByNames")
  class FindByNames {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityNames = List.of("test_resource.create");

      when(capabilityRepository.findAllByNames(capabilityNames)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(List.of(capabilityEntity))).thenReturn(List.of(capability));

      var result = capabilityService.findByNames(capabilityNames);

      assertThat(result).isEqualTo(List.of(capability));
    }
  }

  @Nested
  @DisplayName("findByCapabilitySetId")
  class FindByCapabilitySetId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var entityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilitySetService.get(CAPABILITY_SET_ID)).thenReturn(capabilitySet());
      when(capabilityRepository.findByCapabilitySetId(CAPABILITY_SET_ID, offsetRequest)).thenReturn(entityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByCapabilitySetId(CAPABILITY_SET_ID, offsetRequest);
    }

    @Test
    void positive_includeDummyIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var entityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilitySetService.get(CAPABILITY_SET_ID)).thenReturn(capabilitySet());
      when(capabilityRepository
        .findByCapabilitySetIdIncludeDummy(CAPABILITY_SET_ID, offsetRequest)).thenReturn(entityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
      verify(capabilityRepository).findByCapabilitySetIdIncludeDummy(CAPABILITY_SET_ID, offsetRequest);
    }

    @Test
    void negative_capabilitySetIsNotFound() {
      when(capabilitySetService.get(CAPABILITY_SET_ID)).thenThrow(EntityNotFoundException.class);
      assertThatThrownBy(() -> capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, false, 10, 0))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByCapabilitySetIds")
  class FindByCapabilitySetIds {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntities = List.of(capabilityEntity());
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);

      when(capabilityRepository.findByCapabilitySetIds(capabilitySetIds)).thenReturn(capabilityEntities);
      when(capabilityEntityMapper.convert(capabilityEntities)).thenReturn(List.of(capability));

      var result = capabilityService.findByCapabilitySetIds(capabilitySetIds);

      assertThat(result).containsExactly(capability);
    }
  }

  @Nested
  @DisplayName("checkCapabilityIds")
  class CheckCapabilityIds {

    @Test
    void positive() {
      var capabilityIds = List.of(CAPABILITY_ID);
      var capabilityIdsSet = Set.of(CAPABILITY_ID);
      when(capabilityRepository.findCapabilityIdsByIdIncludeDummy(capabilityIdsSet)).thenReturn(capabilityIdsSet);

      capabilityService.checkIds(capabilityIds);

      verify(capabilityRepository).findCapabilityIdsByIdIncludeDummy(capabilityIdsSet);
    }

    @Test
    void positive_emptyInput() {
      capabilityService.checkIds(emptyList());
      verifyNoInteractions(capabilityRepository);
    }

    @Test
    void negative_capabilityNotFoundById() {
      when(capabilityRepository.findCapabilityIdsByIdIncludeDummy(Set.of(CAPABILITY_ID))).thenReturn(emptySet());

      var capabilityIds = List.of(CAPABILITY_ID);
      assertThatThrownBy(() -> capabilityService.checkIds(capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Capabilities not found by ids: %s", List.of(CAPABILITY_ID));
    }
  }

  @Nested
  @DisplayName("findByIds")
  class FindByIds {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityIds = List.of(CAPABILITY_ID);
      when(capabilityRepository.findAllById(capabilityIds)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByIds(capabilityIds);

      assertThat(result).isEqualTo(List.of(capability));
    }
  }

  @Nested
  @DisplayName("getUserPermissions")
  class GetUserPermissions {

    //desiredPermissions : userPermissions : resolvedPermissions
    static Stream<Arguments> permissionsProvider() {
      return Stream.of(
        Arguments.of(of("ui.all", "be.all"), of("ui.all", "users.all"), of("ui.all")),
        Arguments.of(of("be.*"), of("ui.all", "be.get", "be.post"), of("be.get", "be.post")),
        Arguments.of(of("be.it.*", "ui.all"), of("be.all", "be.it.get", "be.it.post"), of("be.it.get", "be.it.post")),
        Arguments.of(of("be.it.*", "ui.all"), emptyList(), emptyList()),
        Arguments.of(of("be.it.*", "ui.all"), of("be.all", "users.item.all"), emptyList())
      );
    }

    @Test
    void positive() {
      when(userPermissionCacheService.getAllUserPermissions(USER_ID)).thenReturn(List.of(PERMISSION_NAME));
      var result = capabilityService.getUserPermissions(USER_ID, false, emptyList());

      assertThat(result).containsExactly(PERMISSION_NAME);
    }

    @Test
    void positive_onlyVisiblePermissions() {
      var uiPermission = "ui-users.view";
      when(userPermissionCacheService.getAllUserPermissions(USER_ID))
        .thenReturn(List.of(PERMISSION_NAME, uiPermission));
      var result = capabilityService.getUserPermissions(USER_ID, true, emptyList());

      assertThat(result).containsExactly(uiPermission);
    }

    @ParameterizedTest(name = "{index} desired: {0}, user's: {1}, resolved: {2}")
    @MethodSource("permissionsProvider")
    void positive_desiredPermissions(List<String> desiredPerms, List<String> userPerms, List<String> resolvedPerms) {
      // Mock the cache to return the user's permissions
      when(userPermissionCacheService.getAllUserPermissions(USER_ID)).thenReturn(userPerms);

      var result = capabilityService.getUserPermissions(USER_ID, false, desiredPerms);

      assertThat(result).containsExactlyInAnyOrderElementsOf(resolvedPerms);
    }
  }

  @Nested
  @DisplayName("updateApplicationVersion")
  class UpdateApplicationVersion {

    @Test
    void positive() {
      var moduleId = "mod-test-1.0.0";
      capabilityService.updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
      verify(capabilityRepository).updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
    }
  }

  @Nested
  @DisplayName("findByPermissionNames")
  class FindByPermissionNames {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var permissionNames = List.of("test_resource.create");
      when(capabilityRepository.findAllByPermissionNames(permissionNames)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(List.of(capabilityEntity))).thenReturn(List.of(capability));

      var result = capabilityService.findByPermissionNames(permissionNames);

      assertThat(result).containsExactly(capability);
      verify(capabilityRepository).findAllByPermissionNames(permissionNames);
    }

    @Test
    void positive_emptyInput() {
      var result = capabilityService.findByPermissionNames(emptyList());
      assertThat(result).isEmpty();
      verifyNoInteractions(capabilityRepository);
    }
  }

  @Nested
  @DisplayName("findByPermissionNamesIncludeDummy")
  class FindByPermissionNamesIncludeDummy {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var permissionNames = List.of("test_resource.create");
      when(capabilityRepository.findAllByPermissionNamesIncludeDummy(permissionNames))
        .thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(List.of(capabilityEntity))).thenReturn(List.of(capability));

      var result = capabilityService.findByPermissionNamesIncludeDummy(permissionNames);

      assertThat(result).containsExactly(capability);
      verify(capabilityRepository).findAllByPermissionNamesIncludeDummy(permissionNames);
    }

    @Test
    void positive_emptyInput() {
      var result = capabilityService.findByPermissionNamesIncludeDummy(emptyList());
      assertThat(result).isEmpty();
      verifyNoInteractions(capabilityRepository);
    }
  }

  @Nested
  @DisplayName("findByPermissionName")
  class FindByPermissionName {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      when(capabilityRepository.findByPermission(PERMISSION_NAME)).thenReturn(Optional.of(capabilityEntity));
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByPermissionName(PERMISSION_NAME);

      assertThat(result).contains(capability);
    }

    @Test
    void positive_includingTechnicalCapabilities() {
      var capabilityEntity = capabilityEntity();
      var permissionNames = List.of(PERMISSION_NAME);
      when(capabilityRepository.findAllByPermissionNames(permissionNames)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(List.of(capabilityEntity))).thenReturn(List.of(technicalCapability()));

      var result = capabilityService.findByPermissionNames(permissionNames);

      assertThat(result).isEqualTo(List.of(technicalCapability()));
    }

    @Test
    void negative_permissionNotFound() {
      when(capabilityRepository.findByPermission(PERMISSION_NAME)).thenReturn(Optional.empty());

      var result = capabilityService.findByPermissionName(PERMISSION_NAME);

      assertThat(result).isEmpty();
    }
  }
}
