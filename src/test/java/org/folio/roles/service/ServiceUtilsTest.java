package org.folio.roles.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.service.ServiceUtils.comparatorById;
import static org.folio.roles.service.ServiceUtils.merge;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.roles.domain.entity.Identifiable;
import org.folio.roles.service.ServiceUtils.UpdatePair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServiceUtilsTest {

  @ParameterizedTest
  @MethodSource("mergePositiveProvider")
  void merge_positive(Collection<Identifiable<UUID>> incoming, Collection<Identifiable<UUID>> stored,
    List<Identifiable<UUID>> expectedAdded, List<UpdatePair<? extends Identifiable<UUID>>> expectedUpdated,
    List<Identifiable<UUID>> expectedDeleted) {

    var added = new ArrayList<Identifiable<UUID>>();
    var updated = new ArrayList<UpdatePair<? extends Identifiable<UUID>>>();
    var deleted = new ArrayList<Identifiable<UUID>>();

    merge(incoming, stored, comparatorById(), added::add, updated::add, deleted::add);

    assertThat(added).containsExactlyInAnyOrderElementsOf(expectedAdded);
    assertThat(updated).containsExactlyInAnyOrderElementsOf(expectedUpdated);
    assertThat(deleted).containsExactlyInAnyOrderElementsOf(expectedDeleted);
  }

  public static Stream<Arguments> mergePositiveProvider() {
    var newRecord1 = new IdRecord(null, "value1");
    var newRecord2 = new IdRecord(null, "value2");
    var newRecord3 = new IdRecord(UUID.randomUUID(), "value3");

    var storedRecord1 = new IdRecord(UUID.randomUUID(), "value4");
    var storedRecord2 = new IdRecord(UUID.randomUUID(), "value5");
    var storedRecord3 = new IdRecord(UUID.randomUUID(), "value6");

    var sharedRecord1 = new IdRecord(UUID.randomUUID(), "value7");
    var sharedRecord2 = new IdRecord(UUID.randomUUID(), "value8");

    return Stream.of(
      arguments( // incoming and stored are null
        null, null,
        emptyList(), emptyList(), emptyList()),

      arguments( // incoming only
        List.of(newRecord1, newRecord2, newRecord3), null,
        List.of(newRecord1, newRecord2, newRecord3), emptyList(), emptyList()),

      arguments( // stored only
        null, List.of(storedRecord1, storedRecord2, storedRecord3),
        emptyList(), emptyList(), List.of(storedRecord1, storedRecord2, storedRecord3)),

      arguments( // incoming and stored w/o shared records
        List.of(newRecord1, newRecord2, newRecord3), List.of(storedRecord1, storedRecord2, storedRecord3),
        List.of(newRecord1, newRecord2, newRecord3), emptyList(), List.of(storedRecord1, storedRecord2, storedRecord3)),

      arguments( // incoming and stored w/ shared records, but nothing to deleted
        List.of(newRecord1, newRecord2, newRecord3, sharedRecord1, sharedRecord2),
        List.of(sharedRecord1, sharedRecord2),
        List.of(newRecord1, newRecord2, newRecord3),
        List.of(new UpdatePair<>(sharedRecord1, sharedRecord1), new UpdatePair<>(sharedRecord2, sharedRecord2)),
        emptyList()),

      arguments( // incoming and stored w/ shared records, but nothing to add
        List.of(sharedRecord1, sharedRecord2),
        List.of(storedRecord1, storedRecord2, storedRecord3, sharedRecord1, sharedRecord2),
        emptyList(),
        List.of(new UpdatePair<>(sharedRecord1, sharedRecord1), new UpdatePair<>(sharedRecord2, sharedRecord2)),
        List.of(storedRecord1, storedRecord2, storedRecord3)),

      arguments( // incoming and stored w/ shared records, all actions applied
        List.of(newRecord1, newRecord2, newRecord3, sharedRecord1, sharedRecord2),
        List.of(storedRecord1, storedRecord2, storedRecord3, sharedRecord1, sharedRecord2),
        List.of(newRecord1, newRecord2, newRecord3),
        List.of(new UpdatePair<>(sharedRecord1, sharedRecord1), new UpdatePair<>(sharedRecord2, sharedRecord2)),
        List.of(storedRecord1, storedRecord2, storedRecord3))
    );
  }

  @Data
  @AllArgsConstructor
  private static class IdRecord implements Identifiable<UUID> {
    private UUID id;
    private String value;
  }
}