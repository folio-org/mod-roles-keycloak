package org.folio.roles.domain.model;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PageResult<T> {

  /**
   * Amount of records in {@link PageResult}.
   */
  private long totalRecords;

  /**
   * Records container.
   */
  private List<T> records;

  /**
   * Creates empty {@link PageResult} object.
   *
   * @param <R> - generic type for result elements
   * @return empty {@link PageResult} object
   */
  public static <R> PageResult<R> empty() {
    return new PageResult<>(0, emptyList());
  }

  /**
   * Creates a {@link PageResult} object from an array of {@link R} values.
   *
   * @param values - array with {@link R} values
   * @param <R> - generic type for result elements
   * @return empty {@link PageResult} object
   */
  @SafeVarargs
  public static <R> PageResult<R> asSinglePage(R... values) {
    return new PageResult<>(values.length, Arrays.asList(values));
  }

  /**
   * Creates a {@link PageResult} object from a list of {@link R} values.
   *
   * @param values - list with {@link R} values
   * @param <R> - generic type for result elements
   * @return empty {@link PageResult} object
   */
  public static <R> PageResult<R> asSinglePage(List<R> values) {
    return new PageResult<>(values.size(), values);
  }

  /**
   * Creates a {@link PageResult} object from a {@link Page} object.
   *
   * @param page - spring-data page result
   * @param <R> - generic type for result elements
   * @return empty {@link PageResult} object
   */
  public static <R> PageResult<R> fromPage(Page<R> page) {
    return new PageResult<>(page.getTotalElements(), page.getContent());
  }

  /**
   * Checks if search result is empty or not.
   *
   * @return true - if search result is empty, false - otherwise.
   */
  public boolean isEmpty() {
    return CollectionUtils.isEmpty(records);
  }
}
