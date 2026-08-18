package org.folio.roles.service.capability;

import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.folio.roles.domain.dto.UserCapabilitySetsQueryRequest;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.repository.projection.UserCapabilitySetNameProjection;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserCapabilitySetQueryServiceTest {

  private static final UUID FIRST_USER_ID = fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_USER_ID = fromString("10000000-0000-0000-0000-000000000002");

  @InjectMocks private UserCapabilitySetQueryService service;

  @Mock private CapabilitySetRepository capabilitySetRepository;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static UserCapabilitySetsQueryRequest request(UUID... userIds) {
    return request(List.of(userIds), null);
  }

  private static UserCapabilitySetsQueryRequest request(List<UUID> userIds, List<String> names) {
    var request = new UserCapabilitySetsQueryRequest().userIds(userIds);
    if (names != null) {
      request.capabilitySetNames(names);
    }
    return request;
  }

  private static UserCapabilitySetNameProjection row(UUID userId, String name) {
    var row = mock(UserCapabilitySetNameProjection.class);
    when(row.getUserId()).thenReturn(userId);
    when(row.getCapabilitySetName()).thenReturn(name);
    return row;
  }

  @Nested
  class Query {

    @Test
    void query_positive_directAndInheritedAssignmentsAreGroupedAndSorted() {
      var request = request(FIRST_USER_ID, SECOND_USER_ID);
      var rows = List.of(
        row(FIRST_USER_ID, "zeta_item.view"),
        row(FIRST_USER_ID, "foo_item.edit"),
        row(SECOND_USER_ID, "role_item.view"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(
        aryEq(new UUID[] {FIRST_USER_ID, SECOND_USER_ID})))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets()).extracting("userId", "capabilitySetNames")
        .containsExactly(
          Tuple.tuple(FIRST_USER_ID, List.of("foo_item.edit", "zeta_item.view")),
          Tuple.tuple(SECOND_USER_ID, List.of("role_item.view")));
    }

    @Test
    void query_positive_unknownUserHasEmptyNames() {
      var request = request(FIRST_USER_ID, SECOND_USER_ID);
      var rows = List.of(row(FIRST_USER_ID, "foo_item.edit"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(
        aryEq(new UUID[] {FIRST_USER_ID, SECOND_USER_ID})))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets().get(1).getUserId()).isEqualTo(SECOND_USER_ID);
      assertThat(result.getUserCapabilitySets().get(1).getCapabilitySetNames()).isEmpty();
    }

    @Test
    void query_positive_duplicateUserIdsProduceOneResultInRequestOrder() {
      var request = request(SECOND_USER_ID, FIRST_USER_ID, SECOND_USER_ID);
      var rows = List.of(row(FIRST_USER_ID, "foo_item.edit"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(
        aryEq(new UUID[] {SECOND_USER_ID, FIRST_USER_ID})))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets()).extracting("userId")
        .containsExactly(SECOND_USER_ID, FIRST_USER_ID);
    }

    @Test
    void query_positive_namesFilterUsesExactMatches() {
      var names = List.of("foo_item.edit", "unknown_item.view");
      var request = request(List.of(FIRST_USER_ID), names);
      var rows = List.of(row(FIRST_USER_ID, "foo_item.edit"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(
        aryEq(new UUID[] {FIRST_USER_ID}), aryEq(names.toArray(String[]::new))))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets().getFirst().getCapabilitySetNames())
        .containsExactly("foo_item.edit");
    }

    @Test
    void query_positive_undefinedNamesSelectsUnfilteredQuery() {
      var request = new UserCapabilitySetsQueryRequest().userIds(List.of(FIRST_USER_ID));
      var rows = List.of(row(FIRST_USER_ID, "foo_item.edit"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(aryEq(new UUID[] {FIRST_USER_ID})))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets().getFirst().getCapabilitySetNames())
        .containsExactly("foo_item.edit");
    }

    @Test
    void query_positive_emptyNamesListSelectsUnfilteredQuery() {
      var request = request(List.of(FIRST_USER_ID), List.of());
      var rows = List.of(row(FIRST_USER_ID, "foo_item.edit"));
      when(capabilitySetRepository.findEffectiveCapabilitySetNames(aryEq(new UUID[] {FIRST_USER_ID})))
        .thenReturn(rows);

      var result = service.query(request);

      assertThat(result.getUserCapabilitySets().getFirst().getCapabilitySetNames())
        .containsExactly("foo_item.edit");
    }

    @Test
    void query_negative_nullUserIdThrowsRequestValidationException() {
      var userIds = new ArrayList<UUID>();
      userIds.add(null);
      var request = request(userIds, null);

      assertThatThrownBy(() -> service.query(request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("User ID must not be null")
        .satisfies(throwable -> {
          var exception = (RequestValidationException) throwable;
          assertThat(exception.getKey()).isEqualTo("userIds");
          assertThat(exception.getValue()).isNull();
        });
    }

    @Test
    void query_negative_nullCapabilitySetNameThrowsRequestValidationException() {
      var names = new ArrayList<String>();
      names.add(null);
      var request = request(List.of(FIRST_USER_ID), names);

      assertThatThrownBy(() -> service.query(request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Capability set name must not be null")
        .satisfies(throwable -> {
          var exception = (RequestValidationException) throwable;
          assertThat(exception.getKey()).isEqualTo("capabilitySetNames");
          assertThat(exception.getValue()).isNull();
        });
    }
  }
}
