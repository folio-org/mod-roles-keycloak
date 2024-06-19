package org.folio.roles.service.capability;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.UserPolicy;

public abstract class AbstractCapabilityEventHandler {

  /**
   * Applies an action for each role or user in found policies.
   *
   * @param policies list of found policies per role or user
   * @param entityIdExtractor - extractor function for identifiers in policy
   * @param idConsumer - consumer function to perform action upon user or role id
   */
  protected void performActionForPolicies(List<Policy> policies,
    Function<Policy, List<UUID>> entityIdExtractor, Consumer<UUID> idConsumer) {
    if (isEmpty(policies)) {
      return;
    }

    for (var policy : policies) {
      entityIdExtractor.apply(policy).forEach(idConsumer);
    }
  }

  /**
   * Extracts role identifiers from role policy.
   *
   * @param rolePolicy - role policy object
   * @return extracted role identifiers
   */
  protected static List<UUID> extractRoleIds(Policy rolePolicy) {
    return Optional.ofNullable(rolePolicy)
      .map(Policy::getRolePolicy)
      .map(RolePolicy::getRoles)
      .stream()
      .flatMap(Collection::stream)
      .map(RolePolicyRole::getId)
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }

  /**
   * Extracts user identifiers from user policy.
   *
   * @param userPolicy - role policy object
   * @return extracted user identifiers
   */
  protected static List<UUID> extractUserIds(Policy userPolicy) {
    return Optional.ofNullable(userPolicy)
      .map(Policy::getUserPolicy)
      .map(UserPolicy::getUsers)
      .stream()
      .flatMap(Collection::stream)
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }
}
