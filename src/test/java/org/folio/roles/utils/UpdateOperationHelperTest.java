package org.folio.roles.utils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class UpdateOperationHelperTest {

  @ParameterizedTest(name = "[{index}] present={0}, new={1}")
  @MethodSource("performUpdateTestDatasource")
  void performUpdate_positive_parameterized(List<String> presentValues, List<String> newValues,
    List<String> consumedNewValues, List<String> consumedDeprecatedValues) {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(presentValues, newValues, "string")
      .consumeNewEntities(newValuesBucket::addAll)
      .consumeDeprecatedEntities(entities -> deprecatedValuesBucket.addAll(entities));

    assertThat(newValuesBucket).containsExactlyElementsOf(consumedNewValues);
    assertThat(deprecatedValuesBucket).containsExactlyElementsOf(consumedDeprecatedValues);
  }

  @Test
  void performUpdate_newValuesOnly() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(null, List.of("s1", "s2"), "string")
      .consumeNewEntities(newValuesBucket::addAll)
      .consumeDeprecatedEntities(entities -> deprecatedValuesBucket.addAll(entities));

    assertThat(newValuesBucket).containsExactly("s1", "s2");
    assertThat(deprecatedValuesBucket).isEmpty();
  }

  @Test
  void performUpdate_secondCallOnMethodIsIgnored() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(List.of("s1", "s2"), List.of("s2", "s3"), "string")
      .consumeNewEntities(newValuesBucket::addAll)
      .consumeNewEntities(newValuesBucket::addAll)
      .consumeDeprecatedEntities(entities -> deprecatedValuesBucket.addAll(entities));

    assertThat(newValuesBucket).containsExactly("s3");
    assertThat(deprecatedValuesBucket).containsExactly("s1");
  }

  @Test
  void performUpdate_newValuesCached() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(List.of("s1", "s2"), List.of("s2", "s3"), "string")
      .consumeAndCacheNewEntities(newValues -> addToListAndReturn(newValues, newValuesBucket))
      .consumeDeprecatedEntities((entities, existingEntities) -> {
        deprecatedValuesBucket.addAll(entities);
        assertThat(existingEntities).containsExactly("s2", "s3");
      });

    assertThat(newValuesBucket).containsExactly("s3");
    assertThat(deprecatedValuesBucket).containsExactly("s1");
  }

  @Test
  void performUpdate_newValuesCacheSkipped() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(List.of("s1", "s2"), List.of("s2"), "string")
      .consumeAndCacheNewEntities(newValues -> addToListAndReturn(newValues, newValuesBucket))
      .consumeDeprecatedEntities((entities, existingEntities) -> {
        deprecatedValuesBucket.addAll(entities);
        assertThat(existingEntities).containsExactly("s2");
      });

    assertThat(newValuesBucket).isEmpty();
    assertThat(deprecatedValuesBucket).containsExactly("s1");
  }

  @Test
  void performUpdate_nothingToDelete() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    UpdateOperationHelper.create(List.of("s1", "s2"), List.of("s1", "s2", "s3"), "string")
      .consumeAndCacheNewEntities(newValues -> addToListAndReturn(newValues, newValuesBucket))
      .consumeDeprecatedEntities((entities, existingEntities) -> deprecatedValuesBucket.addAll(entities));

    assertThat(newValuesBucket).containsExactly("s3");
    assertThat(deprecatedValuesBucket).isEmpty();
  }

  @Test
  void performUpdate_negative() {
    var newValuesBucket = new ArrayList<>();
    var deprecatedValuesBucket = new ArrayList<>();

    var presentValues = List.of("s1", "s2");
    var newValuesList = List.of("s1", "s2");

    UpdateOperationHelper.create(presentValues, newValuesList, "string")
      .consumeAndCacheNewEntities(newValues -> addToListAndReturn(newValues, newValuesBucket))
      .consumeDeprecatedEntities((entities, existingEntities) -> deprecatedValuesBucket.addAll(entities));

    assertThat(newValuesBucket).isEmpty();
    assertThat(deprecatedValuesBucket).isEmpty();
  }

  public static Stream<Arguments> performUpdateTestDatasource() {
    return Stream.of(
      arguments(null, List.of("foo", "bar"), List.of("foo", "bar"), emptyList()),
      arguments(emptyList(), List.of("foo", "bar"), List.of("foo", "bar"), emptyList()),
      arguments(List.of("foo", "bar"), null, emptyList(), List.of("foo", "bar")),
      arguments(List.of("foo", "bar"), emptyList(), emptyList(), List.of("foo", "bar")),
      arguments(List.of("foo", "bar"), List.of("foo", "bar", "baz"), List.of("baz"), emptyList()),
      arguments(List.of("foo", "bar"), List.of("bar", "baz"), List.of("baz"), List.of("foo"))
    );
  }

  private static List<String> addToListAndReturn(List<String> newValues, ArrayList<Object> newValuesBucket) {
    newValuesBucket.addAll(newValues);
    return newValues;
  }
}
