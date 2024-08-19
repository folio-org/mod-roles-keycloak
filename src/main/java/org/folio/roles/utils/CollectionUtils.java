package org.folio.roles.utils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionUtils {

  /**
   * Unions 2 collections, leaving only unique values and returns it as {@link List} object.
   *
   * @param collection1 - first {@link Collection} object
   * @param collection2 - second {@link Collection} object
   * @param <T> - generic type for collection element
   * @return merged collections as {@link List} object
   */
  public static <T> List<T> union(Collection<T> collection1, Collection<T> collection2) {
    return Stream.concat(toStream(collection1), toStream(collection2))
      .distinct()
      .collect(toList());
  }

  /**
   * Transforms given collection to set using provided mapper function.
   *
   * @param collection - collection value to process
   * @param mapper - collection element modifier function
   * @param <T> - generic type for source collection elements
   * @param <R> - generic type for result set elements
   * @return a new set containing modified values
   */
  public static <T, R> Set<R> toSet(Collection<T> collection, Function<T, R> mapper) {
    return toStream(collection)
      .map(mapper)
      .collect(Collectors.toSet());
  }

  /**
   * Collects collection1 and collection2 to a single list, leaving only unique value and preserving order of element.
   *
   * @param collection1 - first {@link Collection} object
   * @param collection2 - second {@link Collection} object
   * @param <T> - generic type for collection elements
   * @return new list with collection1 and collection2 unique element values
   */
  public static <T> List<T> unionUniqueValues(Collection<T> collection1, Collection<T> collection2) {
    var resultSet = new LinkedHashSet<>(emptyIfNull(collection1));
    resultSet.addAll(emptyIfNull(collection2));
    return new ArrayList<>(resultSet);
  }

  /**
   * Returns a difference between list1 and list2 as new list ({@code list1 - list2}.
   *
   * @param list1 - first list value to process
   * @param list2 - second list value to process
   * @param <T> - generic type for list elements
   * @return a new list containing difference between list1 and list2
   */
  public static <T> List<T> difference(List<T> list1, List<T> list2) {
    if (isEmpty(list2)) {
      return emptyIfNull(list1);
    }

    var set = new LinkedHashSet<>(emptyIfNull(list1));
    emptyIfNull(list2).forEach(set::remove);
    return new ArrayList<>(set);
  }

  public static <T> Optional<T> findOne(Collection<T> source) {
    return emptyIfNull(source).size() == 1 ? Optional.ofNullable(source.iterator().next()) : Optional.empty();
  }
}
