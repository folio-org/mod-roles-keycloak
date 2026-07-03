package org.folio.roles.nativex;

import static org.springframework.aot.hint.MemberCategory.ACCESS_DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import org.folio.roles.controller.validation.validator.PolicyValidator;
import org.folio.roles.domain.entity.Auditable;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.CapabilityEndpointEntity;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.entity.EmbeddableEndpoint;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyRoleEntity;
import org.folio.roles.domain.entity.TimePolicyEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.folio.roles.domain.entity.key.UserCapabilityKey;
import org.folio.roles.domain.entity.key.UserCapabilitySetKey;
import org.folio.roles.domain.entity.key.UserRoleKey;
import org.folio.roles.domain.entity.migration.PermissionMigrationErrorEntity;
import org.folio.roles.domain.entity.migration.PermissionMigrationJobEntity;
import org.folio.roles.domain.entity.type.EntityCapabilityAction;
import org.folio.roles.domain.entity.type.EntityCapabilityType;
import org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.domain.model.event.DomainEvent;
import org.folio.roles.domain.model.event.DomainEventType;
import org.folio.roles.domain.model.event.TenantPermissionsChangedEvent;
import org.folio.roles.domain.model.event.UserPermissionsChangedEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.ModuleType;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.keyclock.KeycloakMethodRetryPredicate;
import org.folio.roles.integration.mte.model.MteApplicationDescriptor;
import org.folio.roles.integration.mte.model.MteApplicationDescriptors;
import org.folio.roles.repository.generators.FolioUuidGenerator;
import org.folio.roles.repository.generators.FolioUuidGeneratorImpl;
import org.folio.roles.repository.projection.UserPermissionApplicationProjection;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM native-image reachability hints owned by {@code mod-roles-keycloak}.
 *
 * <p>Wired via {@code @ImportRuntimeHints} on {@link org.folio.roles.RolesApplication}, so the hints are
 * contributed during Spring AOT processing. Low-level library hints (folio-spring base/cql, kafka
 * {@code ResourceEvent} container, Keycloak proxy/DTO binding) are inherited from {@code folio-spring-support}
 * and {@code applications-poc-tools}; this registrar covers only what is app-specific: JPA entities/keys/
 * embeddables that Hibernate AOT may miss, the hypersistence array type and the custom id generator, Liquibase
 * and reference-data resources, Jackson-bound Kafka/MTE payloads, and reflectively-instantiated support types.</p>
 */
public class RolesRuntimeHints implements RuntimeHintsRegistrar {

  /**
   * JPA entities, {@code @MappedSuperclass}, {@code @Embeddable} and {@code @IdClass} types.
   */
  private static final Class<?>[] JPA_TYPES = {
    Auditable.class,
    BasePolicyEntity.class, TimePolicyEntity.class, UserPolicyEntity.class, RolePolicyEntity.class,
    RolePolicyRoleEntity.class,
    CapabilityEntity.class, CapabilitySetEntity.class, CapabilityEndpointEntity.class,
    CapabilityEndpointEntity.CapabilityEndpointPrimaryKey.class,
    EmbeddableEndpoint.class,
    LoadablePermissionEntity.class, LoadableRoleEntity.class,
    PermissionEntity.class,
    RoleCapabilityEntity.class, RoleCapabilitySetEntity.class, RoleEntity.class,
    UserCapabilityEntity.class, UserCapabilitySetEntity.class, UserRoleEntity.class,
    PermissionMigrationErrorEntity.class, PermissionMigrationJobEntity.class,
    EntityCapabilityAction.class, EntityCapabilityType.class, EntityPermissionMigrationJobStatus.class,
    EntityRoleType.class,
    LoadablePermissionKey.class, RoleCapabilityKey.class, RoleCapabilitySetKey.class,
    UserCapabilityKey.class, UserCapabilitySetKey.class, UserRoleKey.class,
  };

  /**
   * Types reflectively instantiated by Hibernate (custom {@code UserType} / {@code IdentifierGenerator}).
   */
  private static final Class<?>[] HIBERNATE_TYPES = {
    ListArrayType.class, FolioUuidGenerator.class, FolioUuidGeneratorImpl.class,
  };

  /** Support types reflectively instantiated by Spring (retry predicate, bean-validation constraint). */
  private static final Class<?>[] SUPPORT_TYPES = {
    KeycloakMethodRetryPredicate.class, PolicyValidator.class,
  };

  /**
   * Types (de)serialized via Jackson: Kafka {@code ResourceEvent} payloads, domain events, MTE payloads.
   */
  private static final Class<?>[] BINDING_TYPES = {
    CapabilityEvent.class, CapabilitySetEvent.class, DomainEvent.class, DomainEventType.class,
    TenantPermissionsChangedEvent.class, UserPermissionsChangedEvent.class,
    org.folio.roles.integration.kafka.model.CapabilityEvent.class,
    CapabilityResultHolder.class, CapabilitySetDescriptor.class, FolioResource.class, ModuleType.class,
    Permission.class,
    MteApplicationDescriptor.class, MteApplicationDescriptors.class,
  };

  /**
   * Liquibase maps changelog XML elements onto {@code Change} objects by reflectively invoking their setters and
   * no-arg constructors, and constructs value/config helper types the same way. Registered by name (liquibase-core
   * is a transitive runtime dependency) so tenant migration can parse the changelog in the native image.
   */
  private static final String[] LIQUIBASE_CHANGE_TYPE_NAMES = {
    "liquibase.change.core.AbstractModifyDataChange", "liquibase.change.core.AddAutoIncrementChange",
    "liquibase.change.core.AddColumnChange", "liquibase.change.core.AddDefaultValueChange",
    "liquibase.change.core.AddForeignKeyConstraintChange", "liquibase.change.core.AddLookupTableChange",
    "liquibase.change.core.AddNotNullConstraintChange", "liquibase.change.core.AddPrimaryKeyChange",
    "liquibase.change.core.AddUniqueConstraintChange", "liquibase.change.core.AlterSequenceChange",
    "liquibase.change.core.CreateIndexChange", "liquibase.change.core.CreateProcedureChange",
    "liquibase.change.core.CreateSequenceChange", "liquibase.change.core.CreateTableChange",
    "liquibase.change.core.CreateViewChange", "liquibase.change.core.DeleteDataChange",
    "liquibase.change.core.DropAllForeignKeyConstraintsChange", "liquibase.change.core.DropColumnChange",
    "liquibase.change.core.DropDefaultValueChange", "liquibase.change.core.DropForeignKeyConstraintChange",
    "liquibase.change.core.DropIndexChange", "liquibase.change.core.DropNotNullConstraintChange",
    "liquibase.change.core.DropPrimaryKeyChange", "liquibase.change.core.DropProcedureChange",
    "liquibase.change.core.DropSequenceChange", "liquibase.change.core.DropTableChange",
    "liquibase.change.core.DropUniqueConstraintChange", "liquibase.change.core.DropViewChange",
    "liquibase.change.core.EmptyChange", "liquibase.change.core.ExecuteShellCommandChange",
    "liquibase.change.core.InsertDataChange", "liquibase.change.core.LoadDataChange",
    "liquibase.change.core.LoadUpdateDataChange", "liquibase.change.core.MergeColumnChange",
    "liquibase.change.core.ModifyDataTypeChange", "liquibase.change.core.OutputChange",
    "liquibase.change.core.RawSQLChange", "liquibase.change.core.RenameColumnChange",
    "liquibase.change.core.RenameSequenceChange", "liquibase.change.core.RenameTableChange",
    "liquibase.change.core.RenameViewChange", "liquibase.change.core.SetColumnRemarksChange",
    "liquibase.change.core.SetTableRemarksChange", "liquibase.change.core.SQLFileChange",
    "liquibase.change.core.StopChange", "liquibase.change.core.TagDatabaseChange",
    "liquibase.change.core.UpdateDataChange",
    // Nested column/constraint config beans and function value types referenced by the setters above.
    "liquibase.change.ColumnConfig", "liquibase.change.AddColumnConfig", "liquibase.change.ConstraintsConfig",
    "liquibase.statement.DatabaseFunction", "liquibase.statement.SequenceNextValueFunction",
    "liquibase.statement.SequenceCurrentValueFunction",
  };

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    registerJpaTypes(hints);
    registerHibernateTypes(hints);
    registerSupportTypes(hints);
    registerBindingTypes(hints);
    registerLiquibaseTypes(hints);
    registerResources(hints);

    // Spring Data interface projection materialised as a JDK dynamic proxy.
    hints.proxies().registerJdkProxy(UserPermissionApplicationProjection.class);
  }

  private void registerJpaTypes(RuntimeHints hints) {
    for (var type : JPA_TYPES) {
      hints.reflection().registerType(type,
        builder -> builder.withMembers(ACCESS_DECLARED_FIELDS, INVOKE_DECLARED_CONSTRUCTORS,
          INVOKE_DECLARED_METHODS));
    }
  }

  private void registerHibernateTypes(RuntimeHints hints) {
    for (var type : HIBERNATE_TYPES) {
      hints.reflection().registerType(type,
        builder -> builder.withMembers(INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_METHODS));
    }
  }

  private void registerSupportTypes(RuntimeHints hints) {
    for (var type : SUPPORT_TYPES) {
      hints.reflection().registerType(type,
        builder -> builder.withMembers(INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_METHODS));
    }
  }

  private void registerBindingTypes(RuntimeHints hints) {
    bindingRegistrar.registerReflectionHints(hints.reflection(), BINDING_TYPES);
  }

  private void registerLiquibaseTypes(RuntimeHints hints) {
    for (var typeName : LIQUIBASE_CHANGE_TYPE_NAMES) {
      hints.reflection().registerType(TypeReference.of(typeName),
        builder -> builder.withMembers(INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS,
          INVOKE_PUBLIC_METHODS, INVOKE_DECLARED_METHODS));
    }
  }

  private void registerResources(RuntimeHints hints) {
    // Liquibase changelog master + all included change sets.
    hints.resources().registerPattern("changelog/changelog-master.xml");
    hints.resources().registerPattern("changelog/changes/*.xml");
    // Liquibase bundles its dbchangelog XSD schemas as classpath resources. With secureParsing=true
    // (the default) it resolves the referenced XSD from the classpath instead of a remote lookup, so
    // the schema files (and build metadata) must be present in the native image or tenant migration fails
    // with XSDLookUpException at runtime.
    hints.resources().registerPattern("www.liquibase.org/xml/ns/dbchangelog/*.xsd");
    hints.resources().registerPattern("liquibase.build.properties");
    // Reference data + permission view/edit mappings loaded via classpath resource scanning.
    hints.resources().registerPattern("reference-data/roles/*.json");
    hints.resources().registerPattern("reference-data/policies/*.json");
    hints.resources().registerPattern("permissions-view-edit-mapping/capabilities/*.json");
    hints.resources().registerPattern("permissions-view-edit-mapping/permissions/*.json");
  }
}
