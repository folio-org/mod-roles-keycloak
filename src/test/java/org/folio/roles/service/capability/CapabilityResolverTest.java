package org.folio.roles.service.capability;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.mockito.Mockito.when;

import org.folio.roles.service.permission.PermissionOverrider;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityResolverTest {

  @InjectMocks private CapabilityResolver capabilityResolver;
  @Mock private PermissionOverrider permissionOverrider;

  @BeforeEach
  void setUp() {
    when(permissionOverrider.getPermissionMappings()).thenReturn(emptyMap());
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("isCapabilityPermissionCorrupted")
  class IsCapabilityPermissionCorrupted {

    @Test
    @DisplayName("positive - should return false for not corrupted capability")
    void positive_shouldReturnFalse() {
      var capability = capability();
      capability.setName("test_resource_items_get.execute");
      capability.setPermission("test.resource.items.get");

      var result = capabilityResolver.isCapabilityPermissionCorrupted(capability);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("negative - should return true for corrupted capability")
    void negative_shouldReturnTrue() {
      var capability = capability();
      capability.setName("wrong.name");
      capability.setPermission("test.resource.item.get");

      var result = capabilityResolver.isCapabilityPermissionCorrupted(capability);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("negative - should throw exception for invalid permission format")
    void negative_shouldThrowException() {
      var capability = capability();
      capability.setName("any.name");
      capability.setPermission("invalid-permission-format");

      assertThatThrownBy(() -> capabilityResolver.isCapabilityPermissionCorrupted(capability))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Permission data is missing required fields: invalid-permission-format");
    }
  }
}
