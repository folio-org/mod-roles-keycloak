package org.folio.roles.nativex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.MemberCategory.ACCESS_DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.domain.entity.key.UserRoleKey;
import org.folio.roles.integration.keyclock.KeycloakMethodRetryPredicate;
import org.folio.roles.repository.projection.UserPermissionApplicationProjection;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

@UnitTest
class RolesRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  RolesRuntimeHintsTest() {
    new RolesRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registerHints_positive_registersJpaEntityFieldReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(PermissionEntity.class)
      .withMemberCategory(ACCESS_DECLARED_FIELDS)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersIdClassKeyReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(UserRoleKey.class)
      .withMemberCategory(ACCESS_DECLARED_FIELDS)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersRetryPredicateReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(KeycloakMethodRetryPredicate.class)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersInterfaceProjectionJdkProxy() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(UserPermissionApplicationProjection.class))
      .accepts(hints);
  }

  @Test
  void registerHints_positive_registersLiquibaseMasterChangelogResource() {
    assertThat(RuntimeHintsPredicates.resource().forResource("changelog/changelog-master.xml")).accepts(hints);
  }

  @Test
  void registerHints_positive_registersLiquibaseXsdResource() {
    assertThat(RuntimeHintsPredicates.resource()
      .forResource("www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd")).accepts(hints);
  }

  @Test
  void registerHints_positive_registersLiquibaseChangeMethodReflection() {
    assertThat(RuntimeHintsPredicates.reflection()
      .onType(TypeReference.of("liquibase.change.core.AddDefaultValueChange"))
      .withMemberCategory(INVOKE_PUBLIC_METHODS)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersPermissionOverriderRecordBinding() {
    assertThat(RuntimeHintsPredicates.reflection()
      .onType(TypeReference.of("org.folio.roles.service.permission.PermissionOverrider$Permission"))).accepts(hints);
  }

  @Test
  void registerHints_positive_registersKeycloakDtoBinding() {
    assertThat(RuntimeHintsPredicates.reflection()
      .onType(TypeReference.of("org.keycloak.representations.idm.ClientRepresentation"))).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection()
      .onType(TypeReference.of("org.keycloak.representations.idm.ProtocolMapperRepresentation"))).accepts(hints);
  }
}
