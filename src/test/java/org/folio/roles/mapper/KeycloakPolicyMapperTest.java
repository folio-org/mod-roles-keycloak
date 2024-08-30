package org.folio.roles.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.roles.support.PolicyUtils.keycloakRolePolicy;
import static org.folio.roles.support.PolicyUtils.keycloakTimePolicy;
import static org.folio.roles.support.PolicyUtils.keycloakUserPolicy;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.PolicyUtils.timePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.mapper.KeycloakPolicyMapper.PolicyMapperContext;
import org.folio.roles.utils.JsonHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakPolicyMapperTest {

  @InjectMocks private KeycloakPolicyMapperImpl mapper;
  @Spy private JsonHelper jsonHelper = new JsonHelper(OBJECT_MAPPER);

  @MethodSource("keycloakPolicyDataProvider")
  @ParameterizedTest(name = "[{index}] name = {0}")
  void toPolicy_parameterized(@SuppressWarnings("unused") String name, PolicyRepresentation kcPolicy, Policy policy) {
    var result = mapper.toPolicy(kcPolicy);
    assertThat(result).isEqualTo(policy);
    if (!kcPolicy.getType().equals("time")) {
      verify(jsonHelper).parse(anyString(), ArgumentMatchers.<TypeReference<List<UUID>>>any());
    }
  }

  @MethodSource("policyDataProvider")
  @ParameterizedTest(name = "[{index}] name = {0}")
  void toKeycloakPolicy_parameterized(@SuppressWarnings("unused") String name,
    Policy policy, PolicyMapperContext context, PolicyRepresentation policyRepresentation) {
    var result = mapper.toKeycloakPolicy(policy, context);
    assertThat(result).isEqualTo(policyRepresentation);
    if (policy.getType() != PolicyType.TIME) {
      verify(jsonHelper).asJsonStringSafe(any());
    }
  }

  private static Stream<Arguments> policyDataProvider() {
    return Stream.of(
      arguments("role policy", rolePolicy(), emptyPolicyMapperContext(), keycloakRolePolicy()),
      arguments("user policy", userPolicy(List.of(USER_ID)), userPolicyMapperContext(), keycloakUserPolicy()),
      arguments("time policy", timePolicy(), emptyPolicyMapperContext(), keycloakTimePolicy())
    );
  }

  private static Stream<Arguments> keycloakPolicyDataProvider() {
    return Stream.of(
      arguments("role policy", keycloakRolePolicy(), rolePolicy()),
      arguments("user policy", keycloakUserPolicy(), userPolicy(List.of(UUID.fromString(KEYCLOAK_USER_ID)))),
      arguments("time policy", keycloakTimePolicy(), timePolicy())
    );
  }

  private static PolicyMapperContext emptyPolicyMapperContext() {
    return new PolicyMapperContext();
  }

  private static PolicyMapperContext userPolicyMapperContext() {
    return new PolicyMapperContext().keycloakUserIds(List.of(KEYCLOAK_USER_ID));
  }
}
