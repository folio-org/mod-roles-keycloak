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
import org.folio.roles.domain.entity.Identifiable;

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

  /*static <K, E extends Identifiable<K>> List<E> mergeAndSave(List<E> incomingEntities, List<E> storedEntities,
    JpaRepository<E, K> repository, BiConsumer<E, E> updateDataMethod) {

    List<E> toDelete = new ArrayList<>();
    List<E> toSave = new ArrayList<>();

    merge(incomingEntities, storedEntities, comparatorById(),
      toSave::add,
      (incoming, stored) -> {
        updateDataMethod.accept(incoming, stored);
        toSave.add(stored);
      },
      toDelete::add);

    repository.flush();

    repository.deleteAllInBatch(toDelete);

    return repository.saveAllAndFlush(toSave);
  }*/

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

  public record UpdatePair<E>(E newItem, E oldItem) {

    public UpdatePair {
      requireNonNull(newItem, "New entity is null");
      requireNonNull(oldItem, "Old entity is null");
    }
  }
}
