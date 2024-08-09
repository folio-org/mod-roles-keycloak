package org.folio.roles.utils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.common.utils.CollectionUtils;
import org.jetbrains.annotations.NotNull;

@Getter
@Log4j2
public final class UpdateOperationHelper<T> {

  private final List<T> newEntities;
  private final List<T> deprecatedEntities;
  private final Collection<T> foundEntities;
  private final Collection<T> newEntitiesResult = new ArrayList<>();

  private UpdateOperationHelper(Collection<T> foundEntities, Collection<T> newEntities, String entityName) {
    var foundValuesSet = createLinkedHashSet(foundEntities);
    var setOfEntitiesToUpdate = createLinkedHashSet(newEntities);
    var newEntitiesList = subtract(setOfEntitiesToUpdate, foundValuesSet);
    var deprecatedEntitiesList = subtract(foundValuesSet, setOfEntitiesToUpdate);

    this.foundEntities = foundEntities;
    this.newEntities = newEntitiesList;
    this.deprecatedEntities = deprecatedEntitiesList;
  }

  public static <R> UpdateOperationHelper<R> create(Collection<R> foundEntities,
    Collection<R> newEntities, String entityName) {
    return new UpdateOperationHelper<>(foundEntities, newEntities, entityName);
  }

  /**
   * Consumes a new entity list using provided function.
   *
   * @param entityListConsumer - entity value consumer as {@link Consumer} instance.
   */
  public UpdateOperationHelper<T> consumeNewEntities(Consumer<List<T>> entityListConsumer) {
    if (isEmpty(this.newEntities)) {
      log.debug("New values list is empty, skipping it");
      return this;
    }

    entityListConsumer.accept(new ArrayList<>(this.newEntities));
    this.newEntities.clear();
    return this;
  }

  /**
   * Consumes and caches a new entity list using provided function.
   *
   * @param entityListFunction - entity value consumer as {@link Function} instance.
   */
  public UpdateOperationHelper<T> consumeAndCacheNewEntities(Function<List<T>, Collection<T>> entityListFunction) {
    if (isEmpty(this.newEntities)) {
      log.debug("New values list is empty, skipping it");
      return this;
    }

    this.newEntitiesResult.addAll(entityListFunction.apply(new ArrayList<>(this.newEntities)));
    this.newEntities.clear();
    return this;
  }

  /**
   * Consumes deprecated entities using provided function.
   *
   * @param entityListConsumer - entity value consumer as {@link Consumer} instance.
   */
  public void consumeDeprecatedEntities(Consumer<List<T>> entityListConsumer) {
    if (isEmpty(this.deprecatedEntities)) {
      log.debug("Deprecated values list is empty, skipping it");
      return;
    }

    entityListConsumer.accept(new ArrayList<>(this.deprecatedEntities));
    this.deprecatedEntities.clear();
  }

  /**
   * Consumes deprecated entities as first parameter, and remaining + created entities as second parameter.
   *
   * <p>Deprecated values consumer has 2 parameters:</p>
   * <ol>
   *   <li>A list with deprecated objects to be deleted</li>
   *   <li>A collection with found and created objects</li>
   * </ol>
   *
   * @param entityListBiConsumer - entity value consumer as {@link BiConsumer} instance
   */
  public void consumeDeprecatedEntities(BiConsumer<List<T>, Collection<T>> entityListBiConsumer) {
    if (isEmpty(this.deprecatedEntities)) {
      log.debug("Deprecated values list is empty, skipping it");
      return;
    }

    var remainingEntities = new LinkedHashSet<>(this.foundEntities);
    remainingEntities.addAll(this.newEntitiesResult);
    this.deprecatedEntities.forEach(remainingEntities::remove);
    entityListBiConsumer.accept(new ArrayList<>(this.deprecatedEntities), remainingEntities);

    this.deprecatedEntities.clear();
  }

  @NotNull
  private static <T> Set<T> createLinkedHashSet(Collection<T> values) {
    return isEmpty(values) ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
  }

  /**
   * Result of subtraction must be mutable.
   */
  @SuppressWarnings("java:S6204")
  private static <T> List<T> subtract(Set<T> a, Set<T> b) {
    return CollectionUtils.toStream(a)
      .filter(value -> !b.contains(value))
      .collect(toList());
  }
}
