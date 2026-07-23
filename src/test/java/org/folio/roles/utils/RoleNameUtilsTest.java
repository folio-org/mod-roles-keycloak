package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class RoleNameUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "Circulation/Administrator",
    "/",
    "/leading",
    "trailing/",
    "a/b/c",
    "3d61e5e0a06e0c8bdd7f6dd0f1e8bb61f28e6b1c/suffix"
  })
  void hasForbiddenCharacters_positive_nameContainsSlash(String roleName) {
    var actual = RoleNameUtils.hasForbiddenCharacters(roleName);

    assertThat(actual).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Circulation Administrator",
    "Cataloger basic-view.v1~2",
    "My Role - v1.0~a",
    "3d61e5e0a06e0c8bdd7f6dd0f1e8bb61f28e6b1c",
    "role\\with\\backslash"
  })
  void hasForbiddenCharacters_negative_compliantName(String roleName) {
    var actual = RoleNameUtils.hasForbiddenCharacters(roleName);

    assertThat(actual).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasForbiddenCharacters_negative_nullOrEmptyName(String roleName) {
    var actual = RoleNameUtils.hasForbiddenCharacters(roleName);

    assertThat(actual).isFalse();
  }

  @Test
  void roleNamePattern_positive_matchesForbiddenCharacterCheck() {
    var pattern = Pattern.compile(RoleNameUtils.ROLE_NAME_PATTERN);
    assertThat("Circulation Administrator").matches(pattern);
    assertThat("Circulation/Administrator").doesNotMatch(pattern);
    assertThat("").doesNotMatch(pattern);
    assertThat("Circulation/Administrator\n").doesNotMatch(pattern);
  }
}
