package org.folio.roles.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import liquibase.integration.spring.SpringResourceAccessor;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RemoveCapabilityDuplicateMigrationTest {

  private static final String OLD_CAPABILITY_NAME = "old-capability";
  private static final String NEW_CAPABILITY_NAME = "new-capability";
  private static final UUID OLD_CAPABILITY_ID = UUID.randomUUID();
  private static final UUID NEW_CAPABILITY_ID = UUID.randomUUID();
  private static final UUID OLD_CAPABILITY_SET_ID = UUID.randomUUID();
  private static final UUID NEW_CAPABILITY_SET_ID = UUID.randomUUID();

  private RemoveCapabilityDuplicateMigration migration;

  @Mock private ApplicationContext mockAppContext;
  @Mock private CapabilityRepository mockCapabilityRepository;
  @Mock private CapabilitySetRepository mockCapabilitySetRepository;
  @Mock private RoleCapabilityRepository mockRoleCapabilityRepository;
  @Mock private UserCapabilityRepository mockUserCapabilityRepository;
  @Mock private RoleCapabilitySetRepository mockRoleCapabilitySetRepository;
  @Mock private UserCapabilitySetRepository mockUserCapabilitySetRepository;
  @Mock private CapabilityEntityMapper mockCapabilityMapper;
  @Mock private CapabilitySetEntityMapper mockCapabilitySetMapper;
  @Mock private CapabilitySetMapper mockCapabilitySetKafkaMapper;
  @Mock private CapabilityService mockCapabilityService;

  @BeforeEach
  void setUp() {
    migration = new RemoveCapabilityDuplicateMigration();
    migration.setOldCapabilityName(OLD_CAPABILITY_NAME);
    migration.setNewCapabilityName(NEW_CAPABILITY_NAME);
    migration.setFileOpener(new SpringResourceAccessor(mockAppContext));

    // Setup default bean retrieval
    lenient().when(mockAppContext.getBean(CapabilityRepository.class)).thenReturn(mockCapabilityRepository);
    lenient().when(mockAppContext.getBean(CapabilitySetRepository.class)).thenReturn(mockCapabilitySetRepository);
    lenient().when(mockAppContext.getBean(RoleCapabilityRepository.class)).thenReturn(mockRoleCapabilityRepository);
    lenient().when(mockAppContext.getBean(UserCapabilityRepository.class)).thenReturn(mockUserCapabilityRepository);
    lenient().when(mockAppContext.getBean(RoleCapabilitySetRepository.class))
      .thenReturn(mockRoleCapabilitySetRepository);
    lenient().when(mockAppContext.getBean(UserCapabilitySetRepository.class))
      .thenReturn(mockUserCapabilitySetRepository);
    lenient().when(mockAppContext.getBean(CapabilityEntityMapper.class)).thenReturn(mockCapabilityMapper);
    lenient().when(mockAppContext.getBean(CapabilitySetEntityMapper.class)).thenReturn(mockCapabilitySetMapper);
    lenient().when(mockAppContext.getBean(CapabilitySetMapper.class)).thenReturn(mockCapabilitySetKafkaMapper);
    lenient().when(mockAppContext.getBean(CapabilityService.class)).thenReturn(mockCapabilityService);
  }

  @Test
  void execute_negative_missingOldCapabilityName() throws Exception {
    migration.setOldCapabilityName(null);

    assertThatThrownBy(() -> migration.execute(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("oldCapabilityName parameter is required");
  }

  @Test
  void execute_negative_missingNewCapabilityName() throws Exception {
    migration.setNewCapabilityName(null);

    assertThatThrownBy(() -> migration.execute(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("newCapabilityName parameter is required");
  }

  @Test
  void execute_negative_blankOldCapabilityName() throws Exception {
    migration.setOldCapabilityName("  ");

    assertThatThrownBy(() -> migration.execute(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("oldCapabilityName parameter is required");
  }

  @Test
  void execute_negative_blankNewCapabilityName() throws Exception {
    migration.setNewCapabilityName("");

    assertThatThrownBy(() -> migration.execute(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("newCapabilityName parameter is required");
  }

  @Test
  void execute_positive_idempotent_bothNotFound() throws Exception {
    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME)));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME)));

    migration.execute(null);

    verify(mockCapabilityRepository).findByName(OLD_CAPABILITY_NAME);
    verify(mockCapabilitySetRepository).findByName(OLD_CAPABILITY_NAME);
    verify(mockCapabilityRepository).findByName(NEW_CAPABILITY_NAME);
    verify(mockCapabilitySetRepository).findByName(NEW_CAPABILITY_NAME);
    verify(mockAppContext, never()).publishEvent(any());
  }

  @Test
  void execute_positive_newCapabilityNotFound_shouldSkip() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME)));

    migration.execute(null);

    verify(mockAppContext, never()).publishEvent(any());
    verify(mockRoleCapabilityRepository, never()).findAllByCapabilityId(any());
  }

  @Test
  void execute_positive_newCapabilitySetNotFound_shouldSkip() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME)));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());

    migration.execute(null);

    verify(mockAppContext, never()).publishEvent(any());
    verify(mockRoleCapabilitySetRepository, never()).findAllByCapabilitySetId(any());
  }

  @Test
  void execute_positive_fullMigration() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    // Setup empty relations for simplicity in this test
    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());
    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    // Setup mappers for event publishing
    var capabilityDto = new Capability();
    capabilityDto.setId(OLD_CAPABILITY_ID);
    capabilityDto.setName(OLD_CAPABILITY_NAME);
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);

    var capabilitySetDto = new CapabilitySet();
    capabilitySetDto.setId(OLD_CAPABILITY_SET_ID);
    capabilitySetDto.setName(OLD_CAPABILITY_NAME);
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);

    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());

    var extendedCapabilitySet = new ExtendedCapabilitySet();
    extendedCapabilitySet.setId(OLD_CAPABILITY_SET_ID);
    extendedCapabilitySet.setName(OLD_CAPABILITY_NAME);
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(eq(capabilitySetDto), any()))
      .thenReturn(extendedCapabilitySet);

    migration.execute(null);

    // Verify capability migration
    verify(mockRoleCapabilityRepository).findAllByCapabilityId(OLD_CAPABILITY_ID);
    verify(mockUserCapabilityRepository).findAllByCapabilityId(OLD_CAPABILITY_ID);

    // Verify capability set migration
    verify(mockRoleCapabilitySetRepository).findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID);
    verify(mockUserCapabilitySetRepository).findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID);

    // Verify events published
    var eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(mockAppContext, times(2)).publishEvent(eventCaptor.capture());

    var events = eventCaptor.getAllValues();
    assertThat(events).hasSize(2);
    assertThat(events.get(0)).isInstanceOf(CapabilityEvent.class);
    assertThat(events.get(1)).isInstanceOf(CapabilitySetEvent.class);

    var capabilityEvent = (CapabilityEvent) events.get(0);
    assertThat(capabilityEvent.getOldObject()).isNotNull();
    assertThat(capabilityEvent.getOldObject().getId()).isEqualTo(OLD_CAPABILITY_ID);

    var capabilitySetEvent = (CapabilitySetEvent) events.get(1);
    assertThat(capabilitySetEvent.getOldObject()).isNotNull();
    assertThat(capabilitySetEvent.getOldObject().getId()).isEqualTo(OLD_CAPABILITY_SET_ID);
  }

  @Test
  void execute_positive_migrateRoleCapabilities_mixedScenario() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    // Setup role_capability: 3 relations - 2 should migrate, 1 should be deleted (duplicate)
    var role1Id = UUID.randomUUID();
    var role2Id = UUID.randomUUID();
    var role3Id = UUID.randomUUID();

    var relation1 = createRoleCapabilityEntity(role1Id, OLD_CAPABILITY_ID);
    var relation2 = createRoleCapabilityEntity(role2Id, OLD_CAPABILITY_ID);
    var relation3 = createRoleCapabilityEntity(role3Id, OLD_CAPABILITY_ID);

    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID))
      .thenReturn(List.of(relation1, relation2, relation3));

    // Role1 and Role2 don't have NEW yet - should migrate
    when(mockRoleCapabilityRepository.existsByRoleIdAndCapabilityId(role1Id, NEW_CAPABILITY_ID)).thenReturn(false);
    when(mockRoleCapabilityRepository.existsByRoleIdAndCapabilityId(role2Id, NEW_CAPABILITY_ID)).thenReturn(false);
    // Role3 already has NEW - should delete OLD
    when(mockRoleCapabilityRepository.existsByRoleIdAndCapabilityId(role3Id, NEW_CAPABILITY_ID)).thenReturn(true);

    when(mockRoleCapabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());
    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    var capabilityDto = new Capability();
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);

    var capabilitySetDto = new CapabilitySet();
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);
    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(any(), any())).thenReturn(new ExtendedCapabilitySet());
    migration.execute(null);

    // Verify migration happened for role1 and role2
    verify(mockRoleCapabilityRepository, times(2)).save(any(RoleCapabilityEntity.class));
    assertThat(relation1.getCapabilityId()).isEqualTo(NEW_CAPABILITY_ID);
    assertThat(relation2.getCapabilityId()).isEqualTo(NEW_CAPABILITY_ID);

    // Verify deletion for role3 (duplicate)
    verify(mockRoleCapabilityRepository).delete(relation3);
  }

  @Test
  void execute_positive_migrateUserCapabilities_mixedScenario() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    // Setup user_capability: 2 relations - 1 migrate, 1 delete
    var user1Id = UUID.randomUUID();
    var user2Id = UUID.randomUUID();

    var userRelation1 = createUserCapabilityEntity(user1Id, OLD_CAPABILITY_ID);
    var userRelation2 = createUserCapabilityEntity(user2Id, OLD_CAPABILITY_ID);

    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID))
      .thenReturn(List.of(userRelation1, userRelation2));

    when(mockUserCapabilityRepository.existsByUserIdAndCapabilityId(user1Id, NEW_CAPABILITY_ID)).thenReturn(false);
    when(mockUserCapabilityRepository.existsByUserIdAndCapabilityId(user2Id, NEW_CAPABILITY_ID)).thenReturn(true);

    when(mockUserCapabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());
    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    var capabilityDto = new Capability();
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);

    var capabilitySetDto = new CapabilitySet();
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);
    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(any(), any())).thenReturn(new ExtendedCapabilitySet());
    migration.execute(null);

    verify(mockUserCapabilityRepository).save(userRelation1);
    assertThat(userRelation1.getCapabilityId()).isEqualTo(NEW_CAPABILITY_ID);
    verify(mockUserCapabilityRepository).delete(userRelation2);
  }

  @Test
  void execute_positive_migrateRoleCapabilitySets_mixedScenario() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    var role1Id = UUID.randomUUID();
    var role2Id = UUID.randomUUID();

    var roleCapSetRelation1 = createRoleCapabilitySetEntity(role1Id, OLD_CAPABILITY_SET_ID);
    var roleCapSetRelation2 = createRoleCapabilitySetEntity(role2Id, OLD_CAPABILITY_SET_ID);

    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID))
      .thenReturn(List.of(roleCapSetRelation1, roleCapSetRelation2));

    when(mockRoleCapabilitySetRepository.existsByRoleIdAndCapabilitySetId(role1Id, NEW_CAPABILITY_SET_ID))
      .thenReturn(false);
    when(mockRoleCapabilitySetRepository.existsByRoleIdAndCapabilitySetId(role2Id, NEW_CAPABILITY_SET_ID))
      .thenReturn(true);

    when(mockRoleCapabilitySetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    var capabilityDto = new Capability();
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);

    var capabilitySetDto = new CapabilitySet();
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);
    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(any(), any())).thenReturn(new ExtendedCapabilitySet());
    migration.execute(null);

    verify(mockRoleCapabilitySetRepository).save(roleCapSetRelation1);
    assertThat(roleCapSetRelation1.getCapabilitySetId()).isEqualTo(NEW_CAPABILITY_SET_ID);
    verify(mockRoleCapabilitySetRepository).delete(roleCapSetRelation2);
  }

  @Test
  void execute_positive_migrateUserCapabilitySets_mixedScenario() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    var user1Id = UUID.randomUUID();
    var user2Id = UUID.randomUUID();
    var user3Id = UUID.randomUUID();

    var userCapSetRelation1 = createUserCapabilitySetEntity(user1Id, OLD_CAPABILITY_SET_ID);
    var userCapSetRelation2 = createUserCapabilitySetEntity(user2Id, OLD_CAPABILITY_SET_ID);
    var userCapSetRelation3 = createUserCapabilitySetEntity(user3Id, OLD_CAPABILITY_SET_ID);

    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID))
      .thenReturn(List.of(userCapSetRelation1, userCapSetRelation2, userCapSetRelation3));

    when(mockUserCapabilitySetRepository.existsByUserIdAndCapabilitySetId(user1Id, NEW_CAPABILITY_SET_ID))
      .thenReturn(false);
    when(mockUserCapabilitySetRepository.existsByUserIdAndCapabilitySetId(user2Id, NEW_CAPABILITY_SET_ID))
      .thenReturn(false);
    when(mockUserCapabilitySetRepository.existsByUserIdAndCapabilitySetId(user3Id, NEW_CAPABILITY_SET_ID))
      .thenReturn(true);

    when(mockUserCapabilitySetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    var capabilityDto = new Capability();
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);

    var capabilitySetDto = new CapabilitySet();
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);
    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(any(), any())).thenReturn(new ExtendedCapabilitySet());
    migration.execute(null);

    verify(mockUserCapabilitySetRepository, times(2)).save(any(UserCapabilitySetEntity.class));
    assertThat(userCapSetRelation1.getCapabilitySetId()).isEqualTo(NEW_CAPABILITY_SET_ID);
    assertThat(userCapSetRelation2.getCapabilitySetId()).isEqualTo(NEW_CAPABILITY_SET_ID);
    verify(mockUserCapabilitySetRepository).delete(userCapSetRelation3);
  }

  @Test
  void execute_positive_onlyCapabilityExists_noCapabilitySet() throws Exception {
    var oldCapability = createCapabilityEntity(OLD_CAPABILITY_ID, OLD_CAPABILITY_NAME);
    var newCapability = createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME)));

    when(mockRoleCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());
    when(mockUserCapabilityRepository.findAllByCapabilityId(OLD_CAPABILITY_ID)).thenReturn(List.of());

    var capabilityDto = new Capability();
    when(mockCapabilityMapper.convert(oldCapability)).thenReturn(capabilityDto);
    migration.execute(null);

    verify(mockRoleCapabilityRepository).findAllByCapabilityId(OLD_CAPABILITY_ID);
    verify(mockUserCapabilityRepository).findAllByCapabilityId(OLD_CAPABILITY_ID);
    verify(mockRoleCapabilitySetRepository, never()).findAllByCapabilitySetId(any());
    verify(mockUserCapabilitySetRepository, never()).findAllByCapabilitySetId(any());

    var eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(mockAppContext, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isInstanceOf(CapabilityEvent.class);
  }

  @Test
  void execute_positive_onlyCapabilitySetExists_noCapability() throws Exception {
    var oldCapabilitySet = createCapabilitySetEntity(OLD_CAPABILITY_SET_ID, OLD_CAPABILITY_NAME);
    var newCapabilitySet = createCapabilitySetEntity(NEW_CAPABILITY_SET_ID, NEW_CAPABILITY_NAME);

    when(mockCapabilityRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(mockCapabilityRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(
      createCapabilityEntity(NEW_CAPABILITY_ID, NEW_CAPABILITY_NAME)));
    when(mockCapabilitySetRepository.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(mockCapabilitySetRepository.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));

    when(mockRoleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());
    when(mockUserCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID)).thenReturn(List.of());

    var capabilitySetDto = new CapabilitySet();
    when(mockCapabilitySetMapper.convert(oldCapabilitySet)).thenReturn(capabilitySetDto);
    when(mockCapabilityService.findByCapabilitySetIdsIncludeDummy(any())).thenReturn(List.of());
    when(mockCapabilitySetKafkaMapper.toExtendedCapabilitySet(any(), any())).thenReturn(new ExtendedCapabilitySet());
    migration.execute(null);

    verify(mockRoleCapabilityRepository, never()).findAllByCapabilityId(any());
    verify(mockUserCapabilityRepository, never()).findAllByCapabilityId(any());
    verify(mockRoleCapabilitySetRepository).findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID);
    verify(mockUserCapabilitySetRepository).findAllByCapabilitySetId(OLD_CAPABILITY_SET_ID);

    var eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(mockAppContext, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isInstanceOf(CapabilitySetEvent.class);
  }

  private CapabilityEntity createCapabilityEntity(UUID id, String name) {
    var entity = new CapabilityEntity();
    entity.setId(id);
    entity.setName(name);
    return entity;
  }

  private CapabilitySetEntity createCapabilitySetEntity(UUID id, String name) {
    var entity = new CapabilitySetEntity();
    entity.setId(id);
    entity.setName(name);
    return entity;
  }

  private RoleCapabilityEntity createRoleCapabilityEntity(UUID roleId, UUID capabilityId) {
    var entity = new RoleCapabilityEntity();
    entity.setRoleId(roleId);
    entity.setCapabilityId(capabilityId);
    return entity;
  }

  private UserCapabilityEntity createUserCapabilityEntity(UUID userId, UUID capabilityId) {
    var entity = new UserCapabilityEntity();
    entity.setUserId(userId);
    entity.setCapabilityId(capabilityId);
    return entity;
  }

  private RoleCapabilitySetEntity createRoleCapabilitySetEntity(UUID roleId, UUID capabilitySetId) {
    var entity = new RoleCapabilitySetEntity();
    entity.setRoleId(roleId);
    entity.setCapabilitySetId(capabilitySetId);
    return entity;
  }

  private UserCapabilitySetEntity createUserCapabilitySetEntity(UUID userId, UUID capabilitySetId) {
    var entity = new UserCapabilitySetEntity();
    entity.setUserId(userId);
    entity.setCapabilitySetId(capabilitySetId);
    return entity;
  }
}
