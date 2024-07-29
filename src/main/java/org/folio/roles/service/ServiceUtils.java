package org.folio.roles.service;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.entity.Identifiable;

@Log4j2
@UtilityClass
public class ServiceUtils {

  public static <E> Consumer<E> nothing() {
    return (e) -> {};
  }

  public static <K extends Comparable<? super K>, E extends Identifiable<? extends K>> Comparator<E> comparatorById() {
    return Comparator.comparing(Identifiable::getId);
  }

  public static <K, E extends Identifiable<? extends K>> boolean equalIds(E first, E second) {
    return nonNull(first) && nonNull(second) &&
      Objects.equals(first.getId(), second.getId());
  }

  public static <E extends Comparable<E>> void merge(Collection<E> incoming, Collection<E> stored,
    Consumer<E> addMethod, Consumer<UpdatePair<E>> updateMethod, Consumer<E> deleteMethod) {
    merge(incoming, stored, Comparable::compareTo, addMethod, updateMethod, deleteMethod);
  }

  public static <E> void merge(Collection<E> incoming, Collection<E> stored, Comparator<E> comparator,
    Consumer<E> addMethod, Consumer<UpdatePair<E>> updateMethod, Consumer<E> deleteMethod) {

    var storedList = new ArrayList<>(emptyIfNull(stored));

    var incomingList = new ArrayList<>(emptyIfNull(incoming));
    incomingList.sort(comparator);

    storedList.forEach(s -> {
      int idx = Collections.binarySearch(incomingList, s, comparator);

      if (idx >= 0) { // updating
        updateMethod.accept(new UpdatePair<>(incomingList.get(idx), s));

        incomingList.remove(idx);
      } else { // removing
        deleteMethod.accept(s);
      }
    });

    incomingList.forEach(addMethod); // what is left in the incoming has to be inserted
  }

  public static <E> void mergeInBatch(Collection<E> incoming, Collection<E> stored, Comparator<E> comparator,
    Consumer<Collection<E>> addAllMethod, Consumer<Collection<UpdatePair<E>>> updateAllMethod,
    Consumer<Collection<E>> deleteAllMethod) {

    var added = new ArrayList<E>();
    var updated = new ArrayList<UpdatePair<E>>();
    var deleted = new ArrayList<E>();
    merge(incoming, stored, comparator,
      added::add, updated::add, deleted::add);
    log.debug("Merge result: created = {}, updated = {}, deleted = {}", added, updated, deleted);

    deleteAllMethod.accept(deleted);
    addAllMethod.accept(added);
    updateAllMethod.accept(updated);
  }

  public record UpdatePair<E>(E newItem, E oldItem) {

    public UpdatePair {
      requireNonNull(newItem, "New entity is null");
      requireNonNull(oldItem, "Old entity is null");
    }
  }
}
