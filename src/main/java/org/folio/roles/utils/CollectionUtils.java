package org.folio.roles.utils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

  public static <T> Optional<T> findOne(Collection<T> source) {
    return emptyIfNull(source).size() == 1 ? Optional.ofNullable(source.iterator().next()) : Optional.empty();
  }
}
