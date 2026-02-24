package org.folio.roles.domain.entity;

import org.jspecify.annotations.Nullable;

public interface Identifiable<K> {

  @Nullable
  K getId();

  void setId(@Nullable K id);
}
