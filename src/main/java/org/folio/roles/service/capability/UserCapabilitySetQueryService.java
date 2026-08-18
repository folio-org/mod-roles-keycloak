package org.folio.roles.service.capability;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.UserCapabilitySetNames;
import org.folio.roles.domain.dto.UserCapabilitySetsQueryRequest;
import org.folio.roles.domain.dto.UserCapabilitySetsQueryResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.repository.projection.UserCapabilitySetNameProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCapabilitySetQueryService {

  private final CapabilitySetRepository capabilitySetRepository;

  /**
   * Queries effective capability-set names for the requested users.
   *
   * <p>Duplicate user ids collapse to one result entry, in request order. Names are not deduplicated
   * here - the repository query does that - but they are sorted so the response is deterministic regardless of the
   * database collation.</p>
   *
   * @param request query request
   * @return effective capability-set names grouped by user
   */
  public UserCapabilitySetsQueryResult query(UserCapabilitySetsQueryRequest request) {
    checkUserIdNotNull(request);
    checkCapabilitySetNameNotNull(request);

    var userIds = new LinkedHashSet<>(request.getUserIds());
    var userIdArray = userIds.toArray(UUID[]::new);
    var names = request.getCapabilitySetNames();

    var rows = isEmpty(names)
      ? capabilitySetRepository.findEffectiveCapabilitySetNames(userIdArray)
      : capabilitySetRepository.findEffectiveCapabilitySetNames(userIdArray, names.toArray(String[]::new));

    var namesByUser = groupSortedByUser(rows);
    return buildResponse(userIds, namesByUser);
  }

  private static UserCapabilitySetsQueryResult buildResponse(LinkedHashSet<UUID> userIds,
    Map<UUID, List<String>> namesByUser) {
    return new UserCapabilitySetsQueryResult().userCapabilitySets(mapCapabilitySetsToUser(userIds, namesByUser));
  }

  private static List<UserCapabilitySetNames> mapCapabilitySetsToUser(LinkedHashSet<UUID> userIds,
    Map<UUID, List<String>> namesByUser) {
    return toStream(userIds)
      .map(userId -> new UserCapabilitySetNames()
        .userId(userId)
        .capabilitySetNames(namesByUser.getOrDefault(userId, List.of())))
      .toList();
  }

  private static void checkCapabilitySetNameNotNull(UserCapabilitySetsQueryRequest request) {
    if (toStream(request.getCapabilitySetNames()).anyMatch(Objects::isNull)) {
      log.debug("Capability set name must not be null in request: {}", request);
      throw new RequestValidationException("Capability set name must not be null", "capabilitySetNames", null);
    }
  }

  private static void checkUserIdNotNull(UserCapabilitySetsQueryRequest request) {
    if (toStream(request.getUserIds()).anyMatch(Objects::isNull)) {
      log.debug("User ID must not be null in request: {}", request);
      throw new RequestValidationException("User ID must not be null", "userIds", null);
    }
  }

  private static Map<UUID, List<String>> groupSortedByUser(List<UserCapabilitySetNameProjection> rows) {
    return toStream(rows)
      .sorted(comparing(UserCapabilitySetNameProjection::getCapabilitySetName))
      .collect(groupingBy(UserCapabilitySetNameProjection::getUserId,
        mapping(UserCapabilitySetNameProjection::getCapabilitySetName, toList())));
  }
}
