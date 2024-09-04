package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class ParseUtilsTest {

  @ParameterizedTest
  @MethodSource("intDataProvider")
  @DisplayName("parseIntSafe_parameterized")
  void parseIntSafe_parameterized(String source, Integer expected) {
    var result = ParseUtils.parseIntSafe(source);
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @DisplayName("parseDateSafe_parameterized")
  @MethodSource("dateSafeDataProvider")
  void parseDateSafe_parameterized(String source, Date expected) {
    var result = ParseUtils.parseDateSafe(source);
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> intDataProvider() {
    return Stream.of(
      arguments(null, null),
      arguments("", null),
      arguments(" ", null),
      arguments("1", 1),
      arguments("-1", -1),
      arguments("2147483647", Integer.MAX_VALUE),
      arguments("10000000000", null),
      arguments("-2147483648", Integer.MIN_VALUE),
      arguments("-10000000000", null),
      arguments("invalid-str", null)
    );
  }

  private static Stream<Arguments> dateSafeDataProvider() {
    return Stream.of(
      arguments(null, null),
      arguments("", null),
      arguments(" ", null),
      arguments("invalid date", null),
      arguments("2024-08-30 12:00:00", Date.from(Instant.ofEpochSecond(1725019200))),
      arguments("2024-08-30T12:00:00", null)
    );
  }
}
